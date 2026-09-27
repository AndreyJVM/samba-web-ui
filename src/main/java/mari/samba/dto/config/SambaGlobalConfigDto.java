package mari.samba.dto.config;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public class SambaGlobalConfigDto {

  @NotBlank(message = "Р Р°Р±РѕС‡Р°СЏ РіСЂСѓРїРїР° РЅРµ РјРѕР¶РµС‚ Р±С‹С‚СЊ РїСѓСЃС‚РѕР№")
  @Pattern(
      regexp = "^[a-zA-Z0-9_.-]+$",
      message = "РќРµРґРѕРїСѓСЃС‚РёРјС‹Рµ СЃРёРјРІРѕР»С‹ РІ РёРјРµРЅРё СЂР°Р±РѕС‡РµР№ РіСЂСѓРїРїС‹")
  private String workgroup = "WORKGROUP";

  private String serverString = "Samba Server";
  private String netbiosName;

  // Р РµР¶РёРј Р°СѓС‚РµРЅС‚РёС„РёРєР°С†РёРё: user, ads
  private String security = "user";

  // РџРѕРІРµРґРµРЅРёРµ РґР»СЏ РЅРµРёР·РІРµСЃС‚РЅС‹С… РїРѕР»СЊР·РѕРІР°С‚РµР»РµР№: Bad User (РґР»СЏ
  // РіРѕСЃС‚РµРІРѕРіРѕ РґРѕСЃС‚СѓРїР°), Never
  private String mapToGuest = "Bad User";

  // РЎРµС‚РµРІС‹Рµ РїСЂРёРІСЏР·РєРё
  private String interfaces;
  private boolean bindInterfacesOnly = false;

  // РћРїС‚РёРјРёР·Р°С†РёСЏ (РѕС‚РєР»СЋС‡РµРЅРёРµ РїСЂРёРЅС‚РµСЂРѕРІ РґР»СЏ С‡РёСЃС‚РѕРіРѕ
  // С„Р°Р№Р»РѕРІРѕРіРѕ СЃРµСЂРІРµСЂР°)
  private boolean loadPrinters = false;
  private boolean disableNetbios = false;

  // Р’РµСЂСЃРёРё РїСЂРѕС‚РѕРєРѕР»Р° SMB (РїРѕ СѓРјРѕР»С‡Р°РЅРёСЋ min=SMB2, max=SMB3)
  private String serverMinProtocol = "SMB2";
  private String serverMaxProtocol = "SMB3";

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
