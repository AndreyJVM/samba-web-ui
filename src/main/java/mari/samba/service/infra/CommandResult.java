package mari.samba.service.infra;

public record CommandResult(int exitCode, String stdout, String stderr) {

  public boolean isSuccess() {
    return exitCode == 0;
  }

  public String getCombinedOutput() {
    if (stderr != null && !stderr.isBlank()) {
      return stderr;
    }
    return stdout != null ? stdout : "";
  }
}
