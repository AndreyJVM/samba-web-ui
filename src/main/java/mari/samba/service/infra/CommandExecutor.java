package mari.samba.service.infra;

import mari.samba.exception.SambaCommandException;

public interface CommandExecutor {

  /**
   * Р’С‹РїРѕР»РЅСЏРµС‚ shell-РєРѕРјР°РЅРґСѓ РЅР° СѓРґР°Р»РµРЅРЅРѕРј С…РѕСЃС‚Рµ.
   *
   * @throws SambaCommandException if the command execution fails.
   */
  String execute(String sessionId, String command) throws SambaCommandException;

  /**
   * Р’С‹РїРѕР»РЅСЏРµС‚ shell-РєРѕРјР°РЅРґСѓ СЃ РїРѕРґР°С‡РµР№ РІС…РѕРґРЅС‹С… РґР°РЅРЅС‹С… РІ stdin.
   *
   * @throws SambaCommandException if the command execution fails.
   */
  String execute(String sessionId, String command, String inputData) throws SambaCommandException;
}
