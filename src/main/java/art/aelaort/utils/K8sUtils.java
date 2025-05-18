package art.aelaort.utils;

import io.fabric8.kubernetes.api.model.IntOrString;

public class K8sUtils {
	public static String unwrap(IntOrString intOrString) {
		if (intOrString == null) {
			return null;
		}

		if (intOrString.getStrVal() != null) {
			return intOrString.getStrVal();
		} else {
			return String.valueOf(intOrString.getIntVal());
		}
	}
}
