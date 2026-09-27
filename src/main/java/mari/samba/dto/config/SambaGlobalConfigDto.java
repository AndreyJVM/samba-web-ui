package mari.samba.dto.config;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public class SambaGlobalConfigDto {

  @NotBlank(message = "Workgroup name cannot be blank")
  @Pattern(regexp = "^[a-zA-Z0-9_.-]+$", message = "Workgroup name contains invalid characters")
  private String workgroup = "WORKGROUP";

  private String serverString = "Samba Server";
  private String netbiosName;

  /** Security mode: user, ads */
  private String security = "user";

  /** Map unknown users to guest account: Bad User, Never */
  private String mapToGuest = "Bad User";

  /** Network interfaces to bind */
  private String interfaces;

  private boolean bindInterfacesOnly = false;

  private boolean loadPrinters = false;
  private boolean disableNetbios = false;

  /** Min/Max SMB protocols */
  private String serverMinProtocol = "SMB2";

  private String serverMaxProtocol = "SMB3";

  // Active Directory & Winbind Properties
  private String realm;
  private boolean winbindUseDefaultDomain = true;
  private String idmapDefaultBackend = "tdb";
  private String idmapDefaultRange = "3000-7999";
  private String idmapDomainBackend = "rid";
  private String idmapDomainRange = "10000-999999";
  private String templateShell = "/bin/bash";

  public String getRealm() {
    return realm;
  }

  public void setRealm(String realm) {
    this.realm = realm;
  }

  public boolean isWinbindUseDefaultDomain() {
    return winbindUseDefaultDomain;
  }

  public void setWinbindUseDefaultDomain(boolean winbindUseDefaultDomain) {
    this.winbindUseDefaultDomain = winbindUseDefaultDomain;
  }

  public String getIdmapDefaultBackend() {
    return idmapDefaultBackend;
  }

  public void setIdmapDefaultBackend(String idmapDefaultBackend) {
    this.idmapDefaultBackend = idmapDefaultBackend;
  }

  public String getIdmapDefaultRange() {
    return idmapDefaultRange;
  }

  public void setIdmapDefaultRange(String idmapDefaultRange) {
    this.idmapDefaultRange = idmapDefaultRange;
  }

  public String getIdmapDomainBackend() {
    return idmapDomainBackend;
  }

  public void setIdmapDomainBackend(String idmapDomainBackend) {
    this.idmapDomainBackend = idmapDomainBackend;
  }

  public String getIdmapDomainRange() {
    return idmapDomainRange;
  }

  public void setIdmapDomainRange(String idmapDomainRange) {
    this.idmapDomainRange = idmapDomainRange;
  }

  public String getTemplateShell() {
    return templateShell;
  }

  public void setTemplateShell(String templateShell) {
    this.templateShell = templateShell;
  }

  public SambaGlobalConfigDto() {}

  public SambaGlobalConfigDto(
      String workgroup,
      String serverString,
      String netbiosName,
      String security,
      String mapToGuest,
      String interfaces,
      boolean bindInterfacesOnly,
      boolean loadPrinters,
      boolean disableNetbios,
      String serverMinProtocol,
      String serverMaxProtocol) {
    this.workgroup = workgroup;
    this.serverString = serverString;
    this.netbiosName = netbiosName;
    this.security = security;
    this.mapToGuest = mapToGuest;
    this.interfaces = interfaces;
    this.bindInterfacesOnly = bindInterfacesOnly;
    this.loadPrinters = loadPrinters;
    this.disableNetbios = disableNetbios;
    this.serverMinProtocol = serverMinProtocol;
    this.serverMaxProtocol = serverMaxProtocol;
  }

  public String getWorkgroup() {
    return workgroup;
  }

  public void setWorkgroup(String workgroup) {
    this.workgroup = workgroup;
  }

  public String getServerString() {
    return serverString;
  }

  public void setServerString(String serverString) {
    this.serverString = serverString;
  }

  public String getNetbiosName() {
    return netbiosName;
  }

  public void setNetbiosName(String netbiosName) {
    this.netbiosName = netbiosName;
  }

  public String getSecurity() {
    return security;
  }

  public void setSecurity(String security) {
    this.security = security;
  }

  public String getMapToGuest() {
    return mapToGuest;
  }

  public void setMapToGuest(String mapToGuest) {
    this.mapToGuest = mapToGuest;
  }

  public String getInterfaces() {
    return interfaces;
  }

  public void setInterfaces(String interfaces) {
    this.interfaces = interfaces;
  }

  public boolean isBindInterfacesOnly() {
    return bindInterfacesOnly;
  }

  public void setBindInterfacesOnly(boolean bindInterfacesOnly) {
    this.bindInterfacesOnly = bindInterfacesOnly;
  }

  public boolean isLoadPrinters() {
    return loadPrinters;
  }

  public void setLoadPrinters(boolean loadPrinters) {
    this.loadPrinters = loadPrinters;
  }

  public boolean isDisableNetbios() {
    return disableNetbios;
  }

  public void setDisableNetbios(boolean disableNetbios) {
    this.disableNetbios = disableNetbios;
  }

  public String getServerMinProtocol() {
    return serverMinProtocol;
  }

  public void setServerMinProtocol(String serverMinProtocol) {
    this.serverMinProtocol = serverMinProtocol;
  }

  public String getServerMaxProtocol() {
    return serverMaxProtocol;
  }

  public void setServerMaxProtocol(String serverMaxProtocol) {
    this.serverMaxProtocol = serverMaxProtocol;
  }
}
