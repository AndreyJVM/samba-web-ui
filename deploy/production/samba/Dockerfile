FROM ubuntu:24.04
ENV DEBIAN_FRONTEND=noninteractive

RUN printf '#!/bin/sh\nexit 101\n' > /usr/sbin/policy-rc.d \
    && chmod 0755 /usr/sbin/policy-rc.d \
    && sed -i 's/^Components:.*/Components: main universe/' /etc/apt/sources.list.d/ubuntu.sources \
    && apt-get update \
    && apt-get install --yes --no-install-recommends \
        samba smbclient openssh-server sudo supervisor winbind krb5-user procps \
    && rm -rf /var/lib/apt/lists/* /etc/ssh/ssh_host_* \
    && mkdir -p /opt/shareguard-node /srv/samba /var/log/samba

COPY smb.conf /etc/samba/smb.conf
COPY sshd_config supervisord.conf /opt/shareguard-node/
COPY --chmod=0755 entrypoint.sh /opt/shareguard-node/entrypoint.sh
COPY --chmod=0755 systemctl /usr/local/bin/systemctl

EXPOSE 22 445
ENTRYPOINT ["/opt/shareguard-node/entrypoint.sh"]
