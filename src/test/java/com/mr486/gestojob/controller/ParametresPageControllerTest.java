package com.mr486.gestojob.controller;

import org.junit.jupiter.api.Test;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.ui.Model;

import static org.assertj.core.api.Assertions.assertThat;

class ParametresPageControllerTest {

    private final ParametresPageController controller = new ParametresPageController();

    @Test
    void parametresView_renvoieLaVue_etActiveLOnglet() {
        Model model = new ExtendedModelMap();
        String view = controller.parametresView(model);

        assertThat(view).isEqualTo("parametres");
        assertThat(model.getAttribute("page_active")).isEqualTo("parametres");
    }
}
