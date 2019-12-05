package eu.aequos.gogas.service;

import com.profesorfalken.jpowershell.PowerShell;
import com.profesorfalken.jpowershell.PowerShellResponse;
import eu.aequos.gogas.exception.DeploymentException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class DeploymentService {

    @Value("${tenant.creation-script.path}")
    private String powerShellScriptPath;

    @Value("${tenant.iis-sites.path}")
    private String iisSitesPath;

    public void deployInstance(String instanceName, String iisSite) throws DeploymentException {
        String scriptParams = new StringBuilder()
                .append("-instanceName \"").append(instanceName).append("\" ")
                .append("-siteName \"gogas_").append(iisSite).append("\" ")
                .append("-webConfigPath \"").append(iisSitesPath).append(iisSite).append("\"")
                .append("-scriptPath \"").append(powerShellScriptPath).append("\"")
                .toString();

        PowerShellResponse powerShellResponse = PowerShell.openSession()
                .executeScript(powerShellScriptPath + "/gogas_create_instance.ps1", scriptParams);

        System.out.println(powerShellResponse.getCommandOutput());

        if (powerShellResponse.isError()) {
            throw new DeploymentException(powerShellResponse.getCommandOutput());
        }
    }
}
