package com.audiometer;

import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.chart.*;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;

public class AudiometerUI extends Application {

    private ScatterChart<Number, Number> audiogram;
    private XYChart.Series<Number, Number> rightEarSeries;
    private XYChart.Series<Number, Number> leftEarSeries;

    private ToggleGroup earGroup;
    private ComboBox<Integer> freqSelector;
    private Slider dbSlider;
    private Label dbValueLabel;

    @Override
    public void start(Stage primaryStage) {
        NumberAxis xAxis = new NumberAxis(125, 8500, 1000);
        xAxis.setLabel("Frequency (Hz)");
        xAxis.setAutoRanging(false);

        NumberAxis yAxis = new NumberAxis(-10, 120, 10);
        yAxis.setLabel("Hearing Threshold (dB HL)");
        yAxis.setAutoRanging(false);

        audiogram = new ScatterChart<>(xAxis, yAxis);
        audiogram.setTitle("Audiogram");
        audiogram.setAnimated(false);

        rightEarSeries = new XYChart.Series<>();
        rightEarSeries.setName("Right Ear (O)");
        leftEarSeries = new XYChart.Series<>();
        leftEarSeries.setName("Left Ear (X)");
        audiogram.getData().add(rightEarSeries);
        audiogram.getData().add(leftEarSeries);

        earGroup = new ToggleGroup();
        ToggleButton rightEarBtn = new ToggleButton("Right Ear");
        rightEarBtn.setUserData("RIGHT");
        rightEarBtn.setToggleGroup(earGroup);
        rightEarBtn.setSelected(true);
        ToggleButton leftEarBtn = new ToggleButton("Left Ear");
        leftEarBtn.setUserData("LEFT");
        leftEarBtn.setToggleGroup(earGroup);

        HBox earBox = new HBox(6, new Label("Ear:"), rightEarBtn, leftEarBtn);
        earBox.setAlignment(Pos.CENTER_LEFT);

        freqSelector = new ComboBox<>();
        freqSelector.getItems().addAll(250, 500, 1000, 2000, 4000, 8000);
        freqSelector.setValue(1000);

        HBox freqBox = new HBox(6, new Label("Frequency:"), freqSelector, new Label("Hz"));
        freqBox.setAlignment(Pos.CENTER_LEFT);

        dbSlider = new Slider(-10, 120, 40);
        dbSlider.setMajorTickUnit(10);
        dbSlider.setMinorTickCount(1);
        dbSlider.setShowTickLabels(true);
        dbSlider.setShowTickMarks(true);
        dbSlider.setSnapToTicks(true);
        dbSlider.setPrefWidth(350);

        dbValueLabel = new Label("40 dB HL");
        dbSlider.valueProperty().addListener((obs, old, val) ->
            dbValueLabel.setText((int) dbSlider.getValue() + " dB HL")
        );

        HBox dbBox = new HBox(6, new Label("Intensity:"), dbSlider, dbValueLabel);
        dbBox.setAlignment(Pos.CENTER_LEFT);

        Button sendToneBtn = new Button("Send Tone");
        Button clearBtn = new Button("Clear");
        clearBtn.setOnAction(e -> {
            rightEarSeries.getData().clear();
            leftEarSeries.getData().clear();
        });

        HBox actionBox = new HBox(8, sendToneBtn, clearBtn);

        VBox controls = new VBox(10, earBox, freqBox, dbBox, actionBox);
        controls.setPadding(new Insets(12));
        controls.setStyle("-fx-background-color: #f4f4f4; -fx-border-color: #cccccc; -fx-border-width: 1 0 0 0;");

        BorderPane root = new BorderPane();
        root.setCenter(audiogram);
        root.setBottom(controls);

        primaryStage.setTitle("Audiometer - Hearing Test");
        primaryStage.setScene(new Scene(root, 950, 750));
        primaryStage.show();
    }

    public int getSelectedFrequency() {
        return freqSelector.getValue();
    }

    public int getSelectedDb() {
        return (int) dbSlider.getValue();
    }

    public String getSelectedEar() {
        return (String) earGroup.getSelectedToggle().getUserData();
    }

    public XYChart.Series<Number, Number> getRightEarSeries() {
        return rightEarSeries;
    }

    public XYChart.Series<Number, Number> getLeftEarSeries() {
        return leftEarSeries;
    }

    public static void main(String[] args) {
        launch(args);
    }
}