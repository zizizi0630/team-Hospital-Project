package healthcare.tool;

import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import healthcare.appointment.AppointmentTools;
import healthcare.tool.method.MethodToolCallbackProvider;

@Configuration
public class HealthcareToolConfig {

    @Bean
    public ToolCallbackProvider appointmentToolCallbackProvider(AppointmentTools appointmentTools,
            healthcare.schedule.ScheduleTools scheduleTools) {
        return MethodToolCallbackProvider.builder()
                .toolObjects(appointmentTools, scheduleTools)
                .build();
    }

}
