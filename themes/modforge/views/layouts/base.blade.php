<!DOCTYPE html>
<html lang="{{ app()->getLocale() }}">
<head>
    <meta charset="utf-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>@yield('title', trans('theme::messages.brand'))</title>
    <meta name="description" content="@yield('description', trans('theme::messages.hero.description'))">

    <link rel="preconnect" href="https://fonts.bunny.net">
    <link href="https://fonts.bunny.net/css?family=inter:400,500,600,700,800,900" rel="stylesheet" />
    <link href="{{ asset('vendor/bootstrap-icons/bootstrap-icons.css') }}" rel="stylesheet">
    <link href="{{ asset('css/app.css') }}" rel="stylesheet">
    <link href="{{ asset('css/base.css') }}" rel="stylesheet">
    @include('elements.theme-color', ['color' => theme_config('accent_color', '#72f1b8')])
    <link href="{{ theme_asset('css/style.css') }}" rel="stylesheet">

    @stack('styles')
</head>
<body>
    @include('elements.navbar')

    <main>
        @yield('app-content')
    </main>

    @include('elements.footer')

    <script src="{{ theme_asset('js/theme.js') }}"></script>
    @stack('scripts')
</body>
</html>
