<nav class="navbar navbar-expand-lg fixed-top mf-navbar">
    <div class="container py-2">
        <a class="mf-brand" href="{{ url('/') }}">
            <span class="mf-brand-mark"><i class="bi bi-hexagon-fill"></i></span>
            <span>{{ trans('theme::messages.brand') }}</span>
        </a>

        <button class="navbar-toggler" type="button" data-bs-toggle="collapse" data-bs-target="#mfNavbar" aria-controls="mfNavbar" aria-expanded="false" aria-label="Toggle navigation">
            <span class="navbar-toggler-icon"></span>
        </button>

        <div class="collapse navbar-collapse" id="mfNavbar">
            <ul class="navbar-nav ms-auto align-items-lg-center gap-lg-3">
                <li class="nav-item"><a class="nav-link mf-nav-link" href="{{ theme_config('cta_primary_url', '/register') }}">{{ trans('theme::messages.nav.play') }}</a></li>
                <li class="nav-item"><a class="nav-link mf-nav-link" href="{{ url('/skin-api') }}">{{ trans('theme::messages.nav.skins') }}</a></li>
                <li class="nav-item"><a class="nav-link mf-nav-link" href="{{ url('/shop') }}">{{ trans('theme::messages.nav.shop') }}</a></li>
                <li class="nav-item"><a class="nav-link mf-nav-link" href="{{ url('/faq') }}">{{ trans('theme::messages.nav.faq') }}</a></li>
                @if (theme_config('discord_url'))
                    <li class="nav-item"><a class="nav-link mf-nav-link" href="{{ theme_config('discord_url') }}" target="_blank" rel="noreferrer">{{ trans('theme::messages.nav.discord') }}</a></li>
                @endif
            </ul>
        </div>
    </div>
</nav>
