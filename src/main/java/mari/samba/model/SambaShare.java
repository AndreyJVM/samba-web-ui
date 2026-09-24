package mari.samba.model;

public class SambaShare {

  private String name;
  private String path;
  private String comment;
  private boolean readOnly;
  private boolean guestOk;
  private boolean browseable;
  private String validUsers;
  private String writeList;
  private String createMask;
  private String directoryMask;
  private String forceUser;
  private String forceGroup;
  private String maxConnections;
  private String hostsAllow;
  private String hostsDeny;

  public SambaShare() {}

  public SambaShare(String name, String path) {
    this.name = name;
    this.path = path;
    this.readOnly = true;
    this.guestOk = false;
    this.browseable = true;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getPath() {
    return path;
  }

  public void setPath(String path) {
    this.path = path;
  }

  public String getComment() {
    return comment;
  }

  public void setComment(String comment) {
    this.comment = comment;
  }

  public boolean isReadOnly() {
    return readOnly;
  }

  public void setReadOnly(boolean readOnly) {
    this.readOnly = readOnly;
  }

  public boolean isGuestOk() {
    return guestOk;
  }

  public void setGuestOk(boolean guestOk) {
    this.guestOk = guestOk;
  }

  public boolean isBrowseable() {
    return browseable;
  }

  public void setBrowseable(boolean browseable) {
    this.browseable = browseable;
  }

  public String getValidUsers() {
    return validUsers;
  }

  public void setValidUsers(String validUsers) {
    this.validUsers = validUsers;
  }

  public String getWriteList() {
    return writeList;
  }

  public void setWriteList(String writeList) {
    this.writeList = writeList;
  }

  public String getCreateMask() {
    return createMask;
  }

  public void setCreateMask(String createMask) {
    this.createMask = createMask;
  }

  public String getDirectoryMask() {
    return directoryMask;
  }

  public void setDirectoryMask(String directoryMask) {
    this.directoryMask = directoryMask;
  }

  public String getForceUser() {
    return forceUser;
  }

  public void setForceUser(String forceUser) {
    this.forceUser = forceUser;
  }

  public String getForceGroup() {
    return forceGroup;
  }

  public void setForceGroup(String forceGroup) {
    this.forceGroup = forceGroup;
  }

  public String getMaxConnections() {
    return maxConnections;
  }

  public void setMaxConnections(String maxConnections) {
    this.maxConnections = maxConnections;
  }

  public String getHostsAllow() {
    return hostsAllow;
  }

  public void setHostsAllow(String hostsAllow) {
    this.hostsAllow = hostsAllow;
  }

  public String getHostsDeny() {
    return hostsDeny;
  }

  public void setHostsDeny(String hostsDeny) {
    this.hostsDeny = hostsDeny;
  }
}
