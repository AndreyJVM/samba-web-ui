package mari.samba.share;

import com.fasterxml.jackson.annotation.JsonProperty;

public record SambaShare(
        @JsonProperty("name") String name,
        @JsonProperty("path") String path,
        @JsonProperty("comment") String comment,
        @JsonProperty("readOnly") boolean readOnly,
        @JsonProperty("guestOk") boolean guestOk,
        @JsonProperty("browseable") boolean browseable,
        @JsonProperty("validUsers") String validUsers,
        @JsonProperty("writeList") String writeList,
        @JsonProperty("createMask") String createMask,
        @JsonProperty("directoryMask") String directoryMask,
        @JsonProperty("forceUser") String forceUser,
        @JsonProperty("forceGroup") String forceGroup,
        @JsonProperty("maxConnections") String maxConnections,
        @JsonProperty("hostsAllow") String hostsAllow,
        @JsonProperty("hostsDeny") String hostsDeny) {

    public SambaShare(String name, String path) {
        this(name, path, null, true, false, true, null, null, null, null, null, null, null, null, null);
    }

    public SambaShare() {
        this(null, null, null, true, false, true, null, null, null, null, null, null, null, null, null);
    }

    public static Builder builder() {
        return new Builder();
    }

    // Getters for backwards compatibility
    public String getName() {
        return name;
    }

    public String getPath() {
        return path;
    }

    public String getComment() {
        return comment;
    }

    public boolean isReadOnly() {
        return readOnly;
    }

    public boolean isGuestOk() {
        return guestOk;
    }

    public boolean isBrowseable() {
        return browseable;
    }

    public String getValidUsers() {
        return validUsers;
    }

    public String getWriteList() {
        return writeList;
    }

    public String getCreateMask() {
        return createMask;
    }

    public String getDirectoryMask() {
        return directoryMask;
    }

    public String getForceUser() {
        return forceUser;
    }

    public String getForceGroup() {
        return forceGroup;
    }

    public String getMaxConnections() {
        return maxConnections;
    }

    public String getHostsAllow() {
        return hostsAllow;
    }

    public String getHostsDeny() {
        return hostsDeny;
    }

    public static class Builder {
        private String name;
        private String path;
        private String comment;
        private boolean readOnly = true;
        private boolean guestOk = false;
        private boolean browseable = true;
        private String validUsers;
        private String writeList;
        private String createMask;
        private String directoryMask;
        private String forceUser;
        private String forceGroup;
        private String maxConnections;
        private String hostsAllow;
        private String hostsDeny;

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder path(String path) {
            this.path = path;
            return this;
        }

        public Builder comment(String comment) {
            this.comment = comment;
            return this;
        }

        public Builder readOnly(boolean readOnly) {
            this.readOnly = readOnly;
            return this;
        }

        public Builder guestOk(boolean guestOk) {
            this.guestOk = guestOk;
            return this;
        }

        public Builder browseable(boolean browseable) {
            this.browseable = browseable;
            return this;
        }

        public Builder validUsers(String validUsers) {
            this.validUsers = validUsers;
            return this;
        }

        public Builder writeList(String writeList) {
            this.writeList = writeList;
            return this;
        }

        public Builder createMask(String createMask) {
            this.createMask = createMask;
            return this;
        }

        public Builder directoryMask(String directoryMask) {
            this.directoryMask = directoryMask;
            return this;
        }

        public Builder forceUser(String forceUser) {
            this.forceUser = forceUser;
            return this;
        }

        public Builder forceGroup(String forceGroup) {
            this.forceGroup = forceGroup;
            return this;
        }

        public Builder maxConnections(String maxConnections) {
            this.maxConnections = maxConnections;
            return this;
        }

        public Builder hostsAllow(String hostsAllow) {
            this.hostsAllow = hostsAllow;
            return this;
        }

        public Builder hostsDeny(String hostsDeny) {
            this.hostsDeny = hostsDeny;
            return this;
        }

        public SambaShare build() {
            return new SambaShare(
                    name,
                    path,
                    comment,
                    readOnly,
                    guestOk,
                    browseable,
                    validUsers,
                    writeList,
                    createMask,
                    directoryMask,
                    forceUser,
                    forceGroup,
                    maxConnections,
                    hostsAllow,
                    hostsDeny);
        }
    }
}
