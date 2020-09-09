package ishgard;
public enum Crafter
{
    CRP(2, "Grade 3 Skybuilders' Bed"),
    BSM(3, "Grade 3 Skybuilders' Oven"),
    ARM(5, "Grade 3 Skybuilders' Lamppost"),
    LTW(7, "Grade 3 Skybuilders' Overalls"),
    GSM(11, "Grade 3 Skybuilders' Brazier"),
    WVR(13, "Grade 3 Skybuilders' Awning"),
    ALC(17, "Grade 3 Skybuilders' Growth Formula"),
    CUL(19, "Grade 3 Skybuilders' Stew");

    private int value;
    private String craftName;
    private Crafter(int value, String name)
    {
        this.value=value;
        this.craftName=name;
    }

    public int getValue()
    {
        return value;
    }

    public String getCraftName()
    {
        return craftName;
    }
}