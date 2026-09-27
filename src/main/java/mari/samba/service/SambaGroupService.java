package mari.samba.service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import mari.samba.dto.group.SambaGroupCreateDto;
import mari.samba.model.SambaGroup;
import mari.samba.service.infra.CommandExecutor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * РЎРµСЂРІРёСЃ РґР»СЏ СѓРїСЂР°РІР»РµРЅРёСЏ Linux-РіСЂСѓРїРїР°РјРё С‡РµСЂРµР· Samba Web UI.
 *
 * <p>Р’СЃРµ РіСЂСѓРїРїС‹, СЃРѕР·РґР°РІР°РµРјС‹Рµ С‡РµСЂРµР· РїСЂРёР»РѕР¶РµРЅРёРµ, РїРѕР»СѓС‡Р°СЋС‚
 * РїСЂРµС„РёРєСЃ {@code smb_}. Р­С‚Рѕ РїРѕР·РІРѕР»СЏРµС‚ Р±РµР·РѕРїР°СЃРЅРѕ РёР·РѕР»РёСЂРѕРІР°С‚СЊ
 * РёС… РѕС‚ СЃРёСЃС‚РµРјРЅС‹С… СЃРµСЂРІРёСЃРЅС‹С… РіСЂСѓРїРї РћРЎ (С‚Р°РєРёС… РєР°Рє root, daemon,
 * sys) Рё РёР·Р±РµР¶Р°С‚СЊ СЃР»СѓС‡Р°Р№РЅС‹С… РїРѕР»РѕРјРѕРє СЃРёСЃС‚РµРјС‹. РџСЂРё РІС‹РІРѕРґРµ РІ
 * РїРѕР»СЊР·РѕРІР°С‚РµР»СЊСЃРєРёР№ РёРЅС‚РµСЂС„РµР№СЃ РїСЂРµС„РёРєСЃ СЃРєСЂС‹РІР°РµС‚СЃСЏ.
 */
@Service
public class SambaGroupService {

  /** Р’РЅСѓС‚СЂРµРЅРЅРёР№ РїСЂРµС„РёРєСЃ РґР»СЏ РіСЂСѓРїРї РІ Linux. */
  private static final String GROUP_PREFIX = "smb_";

  @Autowired private CommandExecutor commandExecutor;

  /**
   * РџРѕР»СѓС‡Р°РµС‚ СЃРїРёСЃРѕРє РІСЃРµС… SMB-РіСЂСѓРїРї, СѓРїСЂР°РІР»СЏРµРјС‹С…
   * РїСЂРёР»РѕР¶РµРЅРёРµРј.
   *
   * @param sessionId РРґРµРЅС‚РёС„РёРєР°С‚РѕСЂ SSH-СЃРµСЃСЃРёРё РїРѕР»СЊР·РѕРІР°С‚РµР»СЏ.
   * @return РЎРїРёСЃРѕРє РѕР±СЉРµРєС‚РѕРІ SambaGroup, РіРґРµ РёРјРµРЅР° РѕС‡РёС‰РµРЅС‹ РѕС‚
   *     РїСЂРµС„РёРєСЃР°. @РІ СЃР»СѓС‡Р°Рµ РѕС€РёР±РєРё РІС‹РїРѕР»РЅРµРЅРёСЏ РєРѕРјР°РЅРґС‹ РїРѕ
   *     SSH.
   */
  public List<SambaGroup> getAllGroups(String sessionId) {
    List<SambaGroup> groups = new ArrayList<>();
    // Р’С‹РїРѕР»РЅСЏРµРј getent group. Р¤РѕСЂРјР°С‚ РІС‹РІРѕРґР°: groupname:password:gid:userlist
    String output = commandExecutor.execute(sessionId, "getent group");
    if (output == null || output.isBlank()) {
      return groups;
    }

    String[] lines = output.split("\\r?\\n");
    for (String line : lines) {
      String[] parts = line.split(":", -1);
      if (parts.length >= 3) {
        String groupName = parts[0];

        if (!groupName.startsWith(GROUP_PREFIX)) {
          continue; // РџСЂРѕРїСѓСЃРєР°РµРј РІСЃРµ СЃРёСЃС‚РµРјРЅС‹Рµ Рё РЅРµ РЅР°С€Рё РіСЂСѓРїРїС‹
        }

        String displayName = groupName.substring(GROUP_PREFIX.length());

        List<String> userList = new ArrayList<>();
        if (parts.length >= 4 && !parts[3].isBlank()) {
          userList = Arrays.asList(parts[3].split(","));
        }

        groups.add(new SambaGroup(displayName, userList));
      }
    }
    return groups;
  }

  /**
   * РЎРѕР·РґР°РµС‚ РЅРѕРІСѓСЋ РіСЂСѓРїРїСѓ РІ РѕРїРµСЂР°С†РёРѕРЅРЅРѕР№ СЃРёСЃС‚РµРјРµ.
   *
   * @param sessionId РРґРµРЅС‚РёС„РёРєР°С‚РѕСЂ SSH-СЃРµСЃСЃРёРё.
   * @param dto DTO СЃ РёРЅС„РѕСЂРјР°С†РёРµР№ Рѕ СЃРѕР·РґР°РІР°РµРјРѕР№ РіСЂСѓРїРїРµ. @РїСЂРё
   *     РѕС€РёР±РєРµ СЃРѕР·РґР°РЅРёСЏ.
   */
  public void createGroup(String sessionId, SambaGroupCreateDto dto) {
    String fullGroupName = GROUP_PREFIX + dto.groupName();
    String command = String.format("sudo groupadd %s", escape(fullGroupName));
    commandExecutor.execute(sessionId, command);
  }

  /**
   * РЈРґР°Р»СЏРµС‚ РіСЂСѓРїРїСѓ РёР· РѕРїРµСЂР°С†РёРѕРЅРЅРѕР№ СЃРёСЃС‚РµРјС‹.
   *
   * @param sessionId РРґРµРЅС‚РёС„РёРєР°С‚РѕСЂ SSH-СЃРµСЃСЃРёРё.
   * @param groupName РРјСЏ РіСЂСѓРїРїС‹ (Р±РµР· РїСЂРµС„РёРєСЃР°). @РїСЂРё РѕС€РёР±РєРµ
   *     СѓРґР°Р»РµРЅРёСЏ.
   */
  public void deleteGroup(String sessionId, String groupName) {
    String fullGroupName = GROUP_PREFIX + groupName;
    String command = String.format("sudo groupdel %s", escape(fullGroupName));
    commandExecutor.execute(sessionId, command);
  }

  /**
   * Р”РѕР±Р°РІР»СЏРµС‚ СЃСѓС‰РµСЃС‚РІСѓСЋС‰РµРіРѕ РїРѕР»СЊР·РѕРІР°С‚РµР»СЏ РІ РіСЂСѓРїРїСѓ.
   *
   * @param sessionId РРґРµРЅС‚РёС„РёРєР°С‚РѕСЂ SSH-СЃРµСЃСЃРёРё.
   * @param username РРјСЏ РїРѕР»СЊР·РѕРІР°С‚РµР»СЏ.
   * @param groupName РРјСЏ РіСЂСѓРїРїС‹ (Р±РµР· РїСЂРµС„РёРєСЃР°). @РїСЂРё РѕС€РёР±РєРµ
   *     РІС‹РїРѕР»РЅРµРЅРёСЏ.
   */
  public void addUserToGroup(String sessionId, String username, String groupName) {
    String fullGroupName = GROUP_PREFIX + groupName;
    String command =
        String.format("sudo gpasswd -a %s %s", escape(username), escape(fullGroupName));
    commandExecutor.execute(sessionId, command);
  }

  /**
   * РСЃРєР»СЋС‡Р°РµС‚ РїРѕР»СЊР·РѕРІР°С‚РµР»СЏ РёР· РіСЂСѓРїРїС‹.
   *
   * @param sessionId РРґРµРЅС‚РёС„РёРєР°С‚РѕСЂ SSH-СЃРµСЃСЃРёРё.
   * @param username РРјСЏ РїРѕР»СЊР·РѕРІР°С‚РµР»СЏ.
   * @param groupName РРјСЏ РіСЂСѓРїРїС‹ (Р±РµР· РїСЂРµС„РёРєСЃР°). @РїСЂРё РѕС€РёР±РєРµ
   *     РІС‹РїРѕР»РЅРµРЅРёСЏ.
   */
  public void removeUserFromGroup(String sessionId, String username, String groupName) {
    String fullGroupName = GROUP_PREFIX + groupName;
    String command =
        String.format("sudo gpasswd -d %s %s", escape(username), escape(fullGroupName));
    commandExecutor.execute(sessionId, command);
  }

  /**
   * Р­РєСЂР°РЅРёСЂСѓРµС‚ Р°СЂРіСѓРјРµРЅС‚ РґР»СЏ Р±РµР·РѕРїР°СЃРЅРѕР№ РїРѕРґСЃС‚Р°РЅРѕРІРєРё РІ
   * shell (Р·Р°С‰РёС‚Р° РѕС‚ Command Injection).
   *
   * @param arg РЎС‚СЂРѕРєР° РґР»СЏ СЌРєСЂР°РЅРёСЂРѕРІР°РЅРёСЏ.
   * @return Р­РєСЂР°РЅРёСЂРѕРІР°РЅРЅР°СЏ СЃС‚СЂРѕРєР° РІ РѕРґРёРЅР°СЂРЅС‹С… РєР°РІС‹С‡РєР°С….
   */
  private String escape(String arg) {
    if (arg == null) return "''";
    return "'" + arg.replace("'", "'\\''") + "'";
  }
}
