package mari.samba.infra;

import java.util.List;

public record PrivilegedCommand(PrivilegedOperation operation, List<String> arguments, String inputData) {

    public PrivilegedCommand {
        arguments = List.copyOf(arguments);
    }
}
