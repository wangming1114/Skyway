FROM ubuntu:24.04
RUN apt-get update && DEBIAN_FRONTEND=noninteractive apt-get install -y --no-install-recommends \
    ca-certificates curl openssl bash iproute2 iptables jq procps python3 python3-dnslib \
    && rm -rf /var/lib/apt/lists/*
WORKDIR /qa
COPY install-test-tools.py prepare-container.sh ./
RUN python3 install-test-tools.py && bash prepare-container.sh
CMD ["sleep", "infinity"]
