package mari.samba.dto.config;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public class SambaGlobalConfigDto {

  @NotBlank(message = "Рабочая группа не может быть пустой")
  @Pattern(regexp = "^[a-zA-Z0-9_.-]+$", message = "Недопустимые символы в имени рабочей группы")
  private String workgroup = "WORKGROUP";

  private String serverString = "Samba Server";
  private String netbiosName;

  // Режим аутентификации: user, ads
  private String security = "user";

  // Поведение для неизвестных пользователей: Bad User (для гостевого доступа), Never
  private String mapToGuest = "Bad User";

  // Сетевые привязки
  private String interfaces;
  private boolean bindInterfacesOnly = false;

  // Оптимизация (отключение принтеров для чистого файлового сервера)
  private boolean loadPrinters = false;
  private boolean disableNetbios = false;

  // Версии протокола SMB (по умолчанию min=SMB2, max=SMB3)
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
