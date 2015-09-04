<?xml version="1.0"?>
<recipe>

    <#if !(hasDependency('com.android.support:support-v4'))>
        <dependency mavenUrl="com.android.support:support-v4:${buildApi}.+"/>
    </#if>

    <execute file="../common/recipe_manifest.xml.ftl" />

    <merge from="root/${resIn}/values/strings.xml.ftl"
             to="${escapeXmlAttribute(resOut)}/values/strings.xml" />

    <merge from="root/${resIn}/values/dimens.xml.ftl"
             to="${escapeXmlAttribute(resOut)}/values/dimens.xml" />

    <instantiate from="root/${resIn}/menu/main.xml.ftl"
            to="${escapeXmlAttribute(resOut)}/menu/${menuName}.xml" />

<#if appCompatActivity>
    <copy from="root/res-v22/drawable"
            to="${escapeXmlAttribute(resOut)}/drawable" />

    <#if !(hasDependency('com.android.support:design'))>
        <dependency mavenUrl="com.android.support:design:${buildApi}.+"/>
    </#if>

    <#if !(hasDependency('com.android.support:appcompat-v7'))>
        <dependency mavenUrl="com.android.support:appcompat-v7:${buildApi}.+"/>
    </#if>

    <execute file="../common/recipe_simple.xml.ftl" />

    <#if hasAppBar>
        <execute file="../common/recipe_app_bar.xml.ftl" />
    <#else>
        <execute file="../common/recipe_no_actionbar.xml.ftl" />
    </#if>

    <instantiate from="root/res-v22/menu/drawer.xml.ftl"
                   to="${escapeXmlAttribute(resOut)}/menu/${drawerMenu}.xml" />
    <instantiate from="root/res-v22/layout/navigation_view.xml.ftl"
                   to="${escapeXmlAttribute(resOut)}/layout/${layoutName}.xml" />
    <instantiate from="root/res-v22/layout/navigation_header.xml.ftl"
                   to="${escapeXmlAttribute(resOut)}/layout/${navHeaderLayoutName}.xml" />

    <instantiate from="root/src-v22/app_package/DrawerActivity.java.ftl"
                   to="${escapeXmlAttribute(srcOut)}/${activityClass}.java" />

    <open file="${escapeXmlAttribute(srcOut)}/${activityClass}.java" />
    <open file="${escapeXmlAttribute(resOut)}/layout/${contentLayoutName}.xml" />
<#else>
    <!-- TODO: switch on Holo Dark v. Holo Light -->
    <copy from="root/res/drawable-hdpi"
            to="${escapeXmlAttribute(resOut)}/drawable-hdpi" />
    <copy from="root/res/drawable-mdpi"
            to="${escapeXmlAttribute(resOut)}/drawable-mdpi" />
    <copy from="root/res/drawable-xhdpi"
            to="${escapeXmlAttribute(resOut)}/drawable-xhdpi" />
    <copy from="root/res/drawable-xxhdpi"
            to="${escapeXmlAttribute(resOut)}/drawable-xxhdpi" />

    <instantiate from="root/res/menu/global.xml.ftl"
                   to="${escapeXmlAttribute(resOut)}/menu/global.xml" />

    <merge from="root/res/values-w820dp/dimens.xml"
             to="${escapeXmlAttribute(resOut)}/values-w820dp/dimens.xml" />

    <instantiate from="root/res/layout/activity_drawer.xml.ftl"
                   to="${escapeXmlAttribute(resOut)}/layout/${layoutName}.xml" />
    <instantiate from="root/res/layout/fragment_navigation_drawer.xml.ftl"
                   to="${escapeXmlAttribute(resOut)}/layout/${navigationDrawerLayout}.xml" />
    <instantiate from="root/res/layout/fragment_simple.xml.ftl"
                   to="${escapeXmlAttribute(resOut)}/layout/${fragmentLayoutName}.xml" />

    <instantiate from="root/src/app_package/DrawerActivity.java.ftl"
                   to="${escapeXmlAttribute(srcOut)}/${activityClass}.java" />
    <instantiate from="root/src/app_package/NavigationDrawerFragment.java.ftl"
                   to="${escapeXmlAttribute(srcOut)}/NavigationDrawerFragment.java" />

    <open file="${escapeXmlAttribute(srcOut)}/${activityClass}.java" />
    <open file="${escapeXmlAttribute(resOut)}/layout/${fragmentLayoutName}.xml" />
</#if>

</recipe>
