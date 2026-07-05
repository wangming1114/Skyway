# VPS Proxy Node Edit Port Design

## Goal

Allow the VPS detail proxy-node editor to change a node's port, apply that change to the remote sing-box config `listen_port`, restart sing-box, and update the node name so it reflects the new port and expiration tag.

## Context

The VPS detail page renders node management through `skyway-ui/src/views/resource/vps/components/ProxyNodePanel.vue`. The edit dialog currently edits expiration, subscription URL, and optional SOCKS5 relay settings, but it does not expose `port`.

The backend update endpoint is `PUT /resource/vps/proxyNode` in `ProxyNodeController`. It already rebuilds node names when expiration changes and uses `VpsSshCommandService` to rename remote config files and restart sing-box. The mapper can update `port`, but the controller currently ignores incoming port changes.

Node names use:

```text
{nodeType}-{address}-{port}-{customerId}-{yyyyMMdd|permanent}
```

## Requirements

- VPS detail node edit must expose an editable port field.
- Port input must accept only integer values from `1` to `65535`.
- Submitting a changed port must update the real remote sing-box `listen_port`.
- Remote sing-box must restart after the config file is patched and renamed.
- The local node row must update `port`, `nodeName`, `expireTime`, and regenerated URL after the remote update succeeds.
- The new node name must include the edited port and edited expiration date or `permanent`.
- A port already used by another node on the same VPS must be rejected before remote changes.
- Existing SOCKS5 relay config must be preserved when the port changes.
- Existing port traffic rules and active per-port rate-limit records must move from the old port to the new port when the port changes.
- If the remote update fails, the database must not be updated.

## Recommended Approach

Use the backend as the authority for port updates.

The frontend submits `port` with the existing edit payload. The backend validates the request, checks same-instance port conflicts, computes the new node base name, patches the remote JSON config, renames the config file, restarts sing-box, updates traffic/rate-limit rules, and only then updates the database.

This avoids local UI state claiming a successful port change when the actual sing-box listener did not change.

## Backend Design

### Controller Flow

`ProxyNodeController.edit` will:

1. Parse optional `port`.
2. Validate range `1..65535` if present.
3. Resolve `newPort` from request or existing node.
4. Reject if another node on the same instance already uses `newPort`.
5. Resolve `newExpireTime`.
6. Build `newNodeName` using existing type/address, `newPort`, customer, and `newExpireTime`.
7. If `port` or expiration changes, call a backend SSH service method that patches the remote config and renames the file.
8. If the node status changes, apply the existing enable/disable rename against the effective new name.
9. Set `row.port`, `row.nodeName`, `row.expireTime`, and `row.url = null` so `ProxyNodeServiceImpl.update` regenerates the share URL from the updated fields.
10. Save local database changes after remote operations have succeeded.

### SSH Service Flow

Add a focused method to `VpsSshCommandService`, for example:

```java
public void updateProxyNodeConfigPortAndName(
    Long instanceId,
    String oldNodeName,
    String newNodeName,
    boolean disabled,
    Integer newPort
) throws IOException
```

This method will:

1. Locate the current config file using `oldNodeName` and the active suffix `.json` or `.json.disabled`.
2. Read the JSON config.
3. Patch `inbounds[*].listen_port` from the current node port to `newPort`.
4. Patch inbound tags and route inbound references where they include the old base filename, so relay routing remains consistent after renaming.
5. Write the patched JSON back to the original file.
6. Rename the config file to `newNodeName + suffix`.
7. Restart sing-box.

If write, rename, or restart fails, the method throws. The controller catches and returns an error without updating the database.

### Rate Limit And Traffic Rules

When `port` changes:

- Existing active `ProxyNodeRateLimit` for the node should move to the new port and reapply remote TC rules.
- Old traffic rules should be removed and new traffic rules ensured.
- If there is no active rate limit, remove old traffic rules and ensure new traffic rules.

This keeps future traffic collection and speed/rate-limit features aligned with the new listener.

## Frontend Design

Update `ProxyNodePanel.vue` edit dialog:

- Add `port` to `editNodeForm`.
- Populate `editNodeForm.port` from the selected row in `openNodeEdit`.
- Add an `el-input-number` under the edit dialog with min `1`, max `65535`, and full width.
- Include `port: editNodeForm.port` in the `updateProxyNode` payload.
- After a successful response, update local row `port` before deriving node name, or preferably refresh via `getList()` and avoid relying on client-only name generation for final state.

The independent proxy-node list page has duplicated edit code. To avoid inconsistent behavior between management entry points, it should receive the same port field and submit behavior.

## Testing Plan

- Add unit tests for sing-box JSON port patching:
  - `listen_port` changes to the new port.
  - inbound tag and route inbound references preserve relay routing after rename.
  - unrelated outbounds and relay config remain unchanged.
- Add controller/service tests for:
  - port range rejection.
  - same-instance port conflict rejection.
  - node name uses edited port and expiration tag.
  - database update is skipped when SSH update throws.
- Add frontend tests or focused static contract tests for:
  - edit form contains a port field.
  - submit payload includes `port`.

## Non-Goals

- Rebuilding the node through the sb add-node menu.
- Changing UUID, protocol, customer, instance, or address during this edit.
- Adding a separate long-running progress UI for edit operations.
