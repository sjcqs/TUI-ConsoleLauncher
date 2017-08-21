package ohi.andre.consolelauncher.commands;

import android.annotation.SuppressLint;
import android.graphics.Color;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import ohi.andre.consolelauncher.commands.main.MainPack;
import ohi.andre.consolelauncher.commands.main.Param;
import ohi.andre.consolelauncher.commands.specific.ParamCommand;
import ohi.andre.consolelauncher.managers.AppsManager;
import ohi.andre.consolelauncher.managers.ContactManager;
import ohi.andre.consolelauncher.managers.FileManager;
import ohi.andre.consolelauncher.managers.FileManager.DirInfo;
import ohi.andre.consolelauncher.managers.WhatsAppManager;
import ohi.andre.consolelauncher.managers.XMLPrefsManager;
import ohi.andre.consolelauncher.managers.music.MusicManager2;
import ohi.andre.consolelauncher.managers.notifications.NotificationManager;
import ohi.andre.consolelauncher.tuils.SimpleMutableEntry;
import ohi.andre.consolelauncher.tuils.Tuils;

@SuppressLint("DefaultLocale")
public class CommandTuils {

    private static FileManager.SpecificExtensionFileFilter extensionFileFilter = new FileManager.SpecificExtensionFileFilter();
    private static FileManager.SpecificNameFileFilter nameFileFilter = new FileManager.SpecificNameFileFilter();

    public static List<XMLPrefsManager.XMLPrefsSave> xmlPrefsEntrys;
    public static List<String> xmlPrefsFiles;

    //	parse a command
    public static Command parse(String input, ExecutePack info, boolean suggestion) throws Exception {
        Command command = new Command();

        String name = CommandTuils.findName(input);
        if (!Tuils.isAlpha(name))
            return null;

        CommandAbstraction cmd = info.commandGroup.getCommandByName(name);
        if (cmd == null) {
            return null;
        }
        command.cmd = cmd;

        input = input.substring(name.length());
        input = input.trim();

        ArrayList<Object> args = new ArrayList<>();
        int nArgs = 0;
        int[] types;

        try {
            if(cmd instanceof ParamCommand) {
                ArgInfo arg = param((MainPack) info, (ParamCommand) cmd, input);
                if(arg == null || !arg.found) {
                    return command;
                }

                input = arg.residualString;
                Param p = (Param) arg.arg;
                types = p.args();

                nArgs++;
                args.add(p);
            } else {
                types = cmd.argType();
            }

            if (types != null) {
                for (int count = 0; count < types.length; count++) {
                    if (input == null) break;

                    input = input.trim();
                    if(input.length() == 0) {
                        break;
                    }

                    ArgInfo arg = CommandTuils.getArg(info, input, types[count], suggestion);
                    if(arg == null) {
                        return null;
                    }

                    if (!arg.found) {
                        command.indexNotFound = cmd instanceof ParamCommand ? count + 1 : count;
                        args.add(input);
                        command.mArgs = args.toArray(new Object[args.size()]);
                        command.nArgs = nArgs;
                        return command;
                    }

                    nArgs += arg.n;
                    args.add(arg.arg);
                    input = arg.residualString;
                }
            }
        } catch (Exception e) {}

        command.mArgs = args.toArray(new Object[args.size()]);
        command.nArgs = nArgs;

        return command;
    }

    //	find command name
    private static String findName(String input) {
        int space = input.indexOf(Tuils.SPACE);

        if (space == -1) {
            return input;
        } else {
            return input.substring(0, space);
        }
    }

    //	find args
    public static ArgInfo getArg(ExecutePack info, String input, int type, boolean suggestion) {
        if (type == CommandAbstraction.FILE && info instanceof MainPack) {
            MainPack pack = (MainPack) info;
            return file(input, pack.currentDirectory);
        } else if (type == CommandAbstraction.CONTACTNUMBER && info instanceof MainPack) {
            MainPack pack = (MainPack) info;
            return contactNumber(input, pack.contacts);
        } else if (type == CommandAbstraction.WHATSAPPNUMBER && info instanceof MainPack){
            MainPack pack = (MainPack) info;
            return contactWhatsApp(input, pack.whatsApp);
        } else if (type == CommandAbstraction.PLAIN_TEXT) {
            return plainText(input);
        } else if (type == CommandAbstraction.VISIBLE_PACKAGE && info instanceof MainPack) {
            MainPack pack = (MainPack) info;
            return activityName(input, pack.appsManager);
        } else if (type == CommandAbstraction.HIDDEN_PACKAGE && info instanceof MainPack) {
            MainPack pack = (MainPack) info;
            return hiddenPackage(input, pack.appsManager);
        } else if (type == CommandAbstraction.TEXTLIST) {
            return textList(input);
        } else if (type == CommandAbstraction.SONG && info instanceof MainPack) {
            MainPack pack = (MainPack) info;
            return song(input, pack.player);
        } else if (type == CommandAbstraction.FILE_LIST && info instanceof MainPack) {
            MainPack pack = (MainPack) info;

            if (suggestion)
                return file(input, pack.currentDirectory);
            else
                return fileList(input, pack.currentDirectory);
        } else if (type == CommandAbstraction.COMMAND) {
            return command(input, info.commandGroup);
        } else if(type == CommandAbstraction.BOOLEAN) {
            return bln(input);
        } else if(type == CommandAbstraction.COLOR) {
            return color(input);
        } else if(type == CommandAbstraction.CONFIG_ENTRY) {
            return configEntry(input);
        } else if(type == CommandAbstraction.CONFIG_FILE) {
            return configFile(input);
        } else if(type == CommandAbstraction.INT) {
            return integer(input);
        } else if(type == CommandAbstraction.DEFAULT_APP) {
            return defaultApp(input, ((MainPack) info).appsManager);
        } else if(type == CommandAbstraction.ALL_PACKAGES) {
            return allPackages(input, ((MainPack) info).appsManager);
        }

        return null;
    }


//	args extractors {

    private static ArgInfo color(String input) {
        input = input.trim();

        int space = input.indexOf(Tuils.SPACE);
        String cl = input.substring(0, space == -1 ? input.length() : space);
        input = space == -1 ? Tuils.EMPTYSTRING : input.substring(space + 1);

        try {
            Color.parseColor(cl);
            return new ArgInfo(cl, input, true, 1);
        } catch (Exception e) {
            return new ArgInfo(null, input, false, 0);
        }
    }

    private static ArgInfo bln(String input) {
        String used, notUsed;
        if(input.contains(Tuils.SPACE)) {
            int space = input.indexOf(Tuils.SPACE);
            used = input.substring(0, space);
            notUsed = input.substring(space + 1);
        } else {
            used = input;
            notUsed = null;
        }

        Object result = used.toLowerCase().equals("true");
        return new ArgInfo(result, notUsed, used.length() > 0, used.length() > 0 ? 1 : 0);
    }

    private static ArgInfo plainText(String input) {
        return new ArgInfo(input, "", true, 1);
    }

    private static ArgInfo textList(String input) {
        if (input == null) {
            return null;
        }

        String[] strings = input.split(Tuils.SPACE + "+");
        List<String> arg = new ArrayList<>(Arrays.asList(strings));

        return new ArgInfo(arg, null, true, arg.size());
    }

    private static ArgInfo command(String string, CommandGroup active) {
        CommandAbstraction abstraction = null;
        try {
            abstraction = active.getCommandByName(string);
        } catch (Exception e) {}

        return new ArgInfo(abstraction, null, abstraction != null, 1);
    }

    @SuppressWarnings("unchecked")
    private static ArgInfo file(String input, File cd) {
        List<String> strings = (List<String>) CommandTuils.textList(input).arg;

        String toVerify = "";
        for (int count = 0; count < strings.size(); count++) {
            toVerify = toVerify.concat(strings.get(count));

            DirInfo info = CommandTuils.getFile(toVerify, cd);
            if (info.file != null && info.notFound == null) {
                while (count-- >= 0)
                    strings.remove(0);

                String residual = Tuils.toPlanString(strings, Tuils.SPACE);
                return new ArgInfo(info.file, residual, true, 1);
            }

            toVerify = toVerify.concat(" ");
        }

        return new ArgInfo(null, input, false, 0);
    }

    @SuppressWarnings("unchecked")
    private static ArgInfo fileList(String input, File cd) {
        List<File> files = new ArrayList<>();
        List<String> strings = (List<String>) CommandTuils.textList(input).arg;

        String toVerify = "";
        for (int count = 0; count < strings.size(); count++) {
            String s = strings.get(count);

            toVerify = toVerify.concat(s);

            DirInfo dir = CommandTuils.getFile(toVerify, cd);
            if (dir.notFound == null) {
                files.add(dir.file);

                toVerify = "";
                continue;
            }

            List<File> tempFiles = CommandTuils.attemptWildcard(dir);
            if (tempFiles != null) {
                files.addAll(tempFiles);

                toVerify = Tuils.EMPTYSTRING;
                continue;
            }

            toVerify = toVerify.concat(Tuils.SPACE);
        }

        if (toVerify.length() > 0)
            return new ArgInfo(null, null, false, 0);

        return new ArgInfo(files, null, files.size() > 0, files.size());
    }

    private static DirInfo getFile(String path, File cd) {
        return FileManager.cd(cd, path);
    }

    private static List<File> attemptWildcard(DirInfo dir) {
        List<File> files;

        FileManager.WildcardInfo info = FileManager.wildcard(dir.notFound);
        if(info == null) {
            return null;
        }

        File cd = dir.file;
        if (!cd.isDirectory()) {
            return null;
        }

        if (info.allExtensions && info.allNames) {
            files = Arrays.asList(cd.listFiles());
        } else if(info.allNames) {
            extensionFileFilter.setExtension(info.extension);
            files = Arrays.asList(cd.listFiles(extensionFileFilter));
        } else if(info.allExtensions) {
            nameFileFilter.setName(info.name);
            files = Arrays.asList(cd.listFiles(nameFileFilter));
        } else {
            return null;
        }

        if (files.size() > 0) {
            return files;
        } else {
            return null;
        }
    }

    private static ArgInfo param(MainPack pack, ParamCommand cmd, String input) {
        if(input == null || input.trim().length() == 0) return null;

        int indexOfFirstSpace = input.indexOf(Tuils.SPACE);
        if (indexOfFirstSpace == -1) {
            indexOfFirstSpace = input.length();
        }

        String param = input.substring(0, indexOfFirstSpace).trim();
        if(param.length() > 0 && !param.startsWith("-")) param = "-".concat(param);

        SimpleMutableEntry<Boolean, Param> sm = cmd.getParam(pack, param);
        Param p = sm.getValue();
        boolean df = sm.getKey();

        return new ArgInfo(p, df ? input : input.substring(indexOfFirstSpace), p != null, p != null ? 1 : 0);
    }

    private static ArgInfo activityName(String input, AppsManager apps) {
        AppsManager.LaunchInfo info = apps.findLaunchInfoWithLabel(input, AppsManager.SHOWN_APPS);
        return new ArgInfo(info, null, info != null, info != null ? 1 : 0);
    }

    private static ArgInfo hiddenPackage(String input, AppsManager apps) {
        AppsManager.LaunchInfo info = apps.findLaunchInfoWithLabel(input, AppsManager.HIDDEN_APPS);
        return new ArgInfo(info, null, info != null, info != null ? 1 : 0);
    }

    private static ArgInfo allPackages(String input, AppsManager apps) {
        AppsManager.LaunchInfo info = apps.findLaunchInfoWithLabel(input, AppsManager.SHOWN_APPS);
        if(info == null) {
            info = apps.findLaunchInfoWithLabel(input, AppsManager.HIDDEN_APPS);
        }

        return new ArgInfo(info, null, info != null, info != null ? 1 : 0);
    }

    private static ArgInfo defaultApp(String input, AppsManager apps) {
        AppsManager.LaunchInfo info = apps.findLaunchInfoWithLabel(input, AppsManager.SHOWN_APPS);
        if(info == null) {
            return new ArgInfo(input, null, true, 1);
        } else {
            return new ArgInfo(info, null, true, 1);
        }
    }

    private static ArgInfo contactNumber(String input, ContactManager contacts) {
        String number;

        if (Tuils.isNumber(input))
            number = input;
        else
            number = contacts.findNumber(input);

        return new ArgInfo(number, null, number != null, 1);
    }


    private static ArgInfo contactWhatsApp(String input, WhatsAppManager w) {
        String number;

        if (input.matches("[0-9]*@s\\.whatsapp\\.net"))
            number = input;
        else
            number = w.findNumber(input);

        return new ArgInfo(number, null, number != null, 1);
    }

    private static ArgInfo song(String input, MusicManager2 music) {
        return new ArgInfo(input, null, true, 1);
    }

    private static ArgInfo configEntry(String input) {
        int index = input.indexOf(Tuils.SPACE);

        if(xmlPrefsEntrys == null) {
            xmlPrefsEntrys = new ArrayList<>();

            for(XMLPrefsManager.XMLPrefsRoot element : XMLPrefsManager.XMLPrefsRoot.values()) Collections.addAll(element.copy);
            Collections.addAll(xmlPrefsEntrys, AppsManager.Options.values());
            Collections.addAll(xmlPrefsEntrys, NotificationManager.Options.values());
        }

        String candidate = index == -1 ? input : input.substring(0,index);
        for(XMLPrefsManager.XMLPrefsSave xs : xmlPrefsEntrys) {
            if(xs.is(candidate)) {
                return new ArgInfo(xs, index == -1 ? null : input.substring(index + 1,input.length()), true, 1);
            }
        }
        return new ArgInfo(null, input, false, 0);
    }

    private static ArgInfo configFile(String input) {
        if(xmlPrefsFiles == null) {
            xmlPrefsFiles = new ArrayList<>();
            for(XMLPrefsManager.XMLPrefsRoot element : XMLPrefsManager.XMLPrefsRoot.values())
                xmlPrefsFiles.add(element.path);
            xmlPrefsFiles.add(AppsManager.PATH);
            xmlPrefsFiles.add(NotificationManager.PATH);
        }

        for(String xs : xmlPrefsFiles) {
            if(xs.equalsIgnoreCase(input)) return new ArgInfo(xs, null, true, 1);
        }
        return new ArgInfo(null, input, false, 0);
    }

    private static ArgInfo integer(String input) {
        int n;
        String s;

        int index = input.indexOf(Tuils.SPACE);
        if(index == -1) s = input;
        else s = input.substring(0, index);

        try {
            n = Integer.parseInt(s);
        } catch (NumberFormatException e) {
            return new ArgInfo(null, input, false, 0);
        }

        return new ArgInfo(n, index == -1 ? null : input.substring(index + 1), true, 1);
    }

    public static boolean isSuRequest(String input) {
        return input.equals("su");
    }

    public static boolean isSuCommand(String input) {
        return input.startsWith("su ");
    }

    public static class ArgInfo {
        public Object arg;
        public String residualString;
        public int n;
        public boolean found;

        public ArgInfo(Object arg, String residualString, boolean found, int nFound) {
            this.arg = arg;
            this.residualString = residualString;
            this.found = found;
            this.n = nFound;
        }
    }

}
