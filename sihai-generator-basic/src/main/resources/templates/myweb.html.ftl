<!DOCTYPE html>

<html>
<head>
    <title>sihai</title>
</head>
<body>
<h1>Hello ${name}!</h1>

<ul>
    <#list menuItems as item>
        <li><a href="${item.url}">${item.label}</a></li>
    </#list>
</ul>

<footer>
    ${currentYear} sihai. All rights reserved.
</footer>
</body>
</html>