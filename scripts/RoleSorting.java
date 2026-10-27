import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.stream.Collectors;
import oneprofile.backend.storage.rolelist.RoleListConfiguration;
import oneprofile.backend.workers.role.RoleReading;
import oneprofile.backend.workers.role.RoleRule;

/**
 * Runs the role rule over normalized titles read from stdin, with the role list on the classpath. Run
 * by role-sorting, never on its own.
 *
 * Reads one normalized title a line, and writes one a line, as
 * title<TAB>role<TAB>reading<TAB>competing roles, comma-separated.
 */
class RoleSorting {

	public static void main(String[] args) throws Exception {
		RoleRule rule = new RoleRule(RoleListConfiguration.shipped());
		PrintStream out = new PrintStream(System.out, false, StandardCharsets.UTF_8);
		BufferedReader in = new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8));
		String title;
		while ((title = in.readLine()) != null) {
			RoleReading reading = rule.read(title);
			out.println(String.join("\t", title, reading.role(), reading.reading().name().toLowerCase(Locale.ROOT),
					reading.competing().stream().sorted().collect(Collectors.joining(","))));
		}
		out.flush();
	}

}
