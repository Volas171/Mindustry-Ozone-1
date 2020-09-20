package Ozone.UI;

import Atom.Random;
import Ozone.Commands.Commands;
import Ozone.Commands.Task.CommandsSpam;
import Ozone.Commands.Task.Task;
import arc.Core;
import arc.graphics.Color;
import arc.graphics.g2d.Draw;
import arc.graphics.g2d.Lines;
import arc.scene.Group;
import arc.scene.event.Touchable;
import arc.scene.ui.TextField;
import arc.scene.ui.layout.Scl;
import arc.scene.ui.layout.Table;
import arc.struct.Queue;
import mindustry.Vars;
import mindustry.core.GameState;
import mindustry.gen.Icon;
import mindustry.gen.Tex;
import mindustry.ui.Styles;
import mindustry.ui.fragments.Fragment;

import java.util.Map;

import static mindustry.Vars.*;

//wont show wtf
public class CommandsListFrag extends Fragment {
    public boolean visible = false;
    private Table logs = new Table().marginRight(30f).marginLeft(20f);
    private Table content = new Table().marginRight(30f).marginLeft(20f);
    private Queue<Task> commandsTask = new arc.struct.Queue<>();
    private float h = 70F;
    private TextField sField;
    private String commands = "";

    @Override
    public void build(Group parent) {
        parent.fill(cont -> {
            cont.visible(() -> visible);
            cont.update(() -> {
                if (!(net.active() && !state.is(GameState.State.menu))) {
                    visible = false;
                    commandsTask.clear();
                }
                if (commandsTask.isEmpty()) return;
                if (!commandsTask.first().isCompleted()) commandsTask.first().update();
                else commandsTask.removeFirst().taskCompleted();
            });


            cont.table(Tex.buttonTrans, pane -> {
                pane.labelWrap(Commands.commandsList.size() + " Commands in total").marginLeft(20);
                pane.row();
                sField = pane.field(commands, (res) -> commands = res).fillX().growX().get();
                pane.button(Icon.zoom, () -> Vars.ui.showTextInput("Commands", "How many times you want to run this",
                        2, "1", true, c -> Vars.ui.showTextInput("Commands", "Delay ? in tick, 20 tick is the lowest standard",
                                6, "100", true, d -> commandsTask.addLast(new CommandsSpam(c, d, commands)))));
                pane.row();
                pane.pane(content).grow().get().setScrollingDisabled(true, false);
                pane.row();
                pane.table(menu -> {
                    menu.defaults().growX().height(50f).fillY();
                    menu.button(Core.bundle.get("close"), this::toggle);
                }).margin(0f).pad(15f).growX();

            }).touchable(Touchable.enabled).margin(14f);
        });
        rebuild();
    }

    public void rebuild() {
        content.clear();
        for (Map.Entry<String, Commands.Command> cl : Commands.commandsList.entrySet()) {
            String name = "[" + Random.getRandomHexColor() + "]" + cl.getKey().replace("-", " ") + "[white]";
            Table button = new Table();
            button.left();
            button.margin(5).marginBottom(10);

            Table table = new Table() {
                @Override
                public void draw() {
                    super.draw();
                    Draw.color(Color.valueOf(Random.getRandomHexColor()));
                    Draw.alpha(parentAlpha);
                    Lines.stroke(Scl.scl(3f));
                    Lines.rect(x, y, width, height);
                    Draw.reset();
                }
            };
            if (cl.getValue().icon != null)
                table.image(cl.getValue().icon).fontScale(3.8f).center().grow();
            button.add(table).size(h);
            button.labelWrap(name).width(170f).pad(10);
            try {
                button.row();
                button.label(() -> cl.getValue().description);
                button.button(Icon.settings, Styles.clearPartiali, () -> ui.showConfirm(name, "are you sure want to run commands: " + name, () -> {
                    String com = cl.getKey();
                    Commands.call(com);
                }));

            } catch (Throwable a) {
                ui.showException(a);
            }
            content.add(button).padBottom(-6).width(350f).maxHeight(h + 14);
            content.row();
            content.image().height(4f).color(Color.magenta).growX();
            content.row();
        }


        content.marginBottom(5);
    }


    public void toggle() {
        visible = !visible;
        if (visible) {
            rebuild();
        } else {
            sField.clearText();
            Core.scene.setKeyboardFocus(null);
        }
    }

}
