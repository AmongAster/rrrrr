@extends('layouts.base')

@section('title', trans('theme::messages.brand'))
@section('description', trans('theme::messages.hero.description'))

@section('app-content')
    <section class="mf-hero container">
        <div class="row align-items-center g-4 g-xl-5">
            <div class="col-lg-7">
                <span class="mf-eyebrow"><i class="bi bi-stars"></i> {{ trans('theme::messages.hero.eyebrow') }}</span>
                <h1>
                    {{ trans('theme::messages.hero.title') }}
                    <span class="mf-gradient-text">{{ theme_config('hero_server_version') }}</span>
                </h1>
                <p class="mf-muted fs-5 mb-4">{{ trans('theme::messages.hero.description') }}</p>

                <div class="mf-hero-actions">
                    <a class="mf-btn-primary" href="{{ theme_config('cta_primary_url', '/register') }}">
                        <i class="bi bi-rocket-takeoff"></i>
                        {{ theme_config('cta_primary_label', trans('theme::messages.nav.play')) }}
                    </a>
                    <a class="mf-btn-secondary" href="{{ theme_config('cta_secondary_url', '/shop') }}">
                        <i class="bi bi-bag-heart"></i>
                        {{ theme_config('cta_secondary_label', trans('theme::messages.nav.shop')) }}
                    </a>
                    @if (theme_config('launcher_download_url'))
                        <a class="mf-btn-secondary" href="{{ theme_config('launcher_download_url') }}">
                            <i class="bi bi-download"></i>
                            {{ trans('theme::messages.hero.download') }}
                        </a>
                    @endif
                </div>
            </div>

            <div class="col-lg-5">
                <div class="mf-panel mf-server-card">
                    <div class="d-flex align-items-center justify-content-between mb-4">
                        <div>
                            <div class="mf-muted text-uppercase small mb-2">{{ trans('theme::messages.hero.server_status') }}</div>
                            <h3 class="mb-0">{{ theme_config('hero_server_ip') }}</h3>
                        </div>
                        <span class="mf-status-dot" aria-hidden="true"></span>
                    </div>

                    <div class="mf-stack flex-column align-items-start mb-4">
                        <span class="mf-badge"><i class="bi bi-controller"></i> {{ theme_config('hero_server_version') }}</span>
                        <span class="mf-badge"><i class="bi bi-shield-check"></i> SkinAPI / Shop / FAQ ready</span>
                    </div>

                    <button class="mf-btn-primary border-0" type="button" data-copy-server="{{ theme_config('hero_server_ip') }}">
                        <i class="bi bi-copy"></i>
                        Copy IP
                    </button>
                </div>
            </div>
        </div>
    </section>

    <section class="container">
        <div class="mf-section">
            <div class="d-flex justify-content-between align-items-end flex-wrap gap-3 mf-section-title">
                <div>
                    <span class="mf-eyebrow">{{ trans('theme::messages.sections.features') }}</span>
                    <h2 class="mt-3 mb-2">{{ trans('theme::messages.sections.community') }}</h2>
                    <p class="mf-muted mb-0">{{ theme_config('faq_intro') }}</p>
                </div>
            </div>

            <div class="row g-4">
                @foreach ([
                    ['icon' => 'bi-cpu', 'title' => theme_config('feature_one_title'), 'text' => theme_config('feature_one_text')],
                    ['icon' => 'bi-gem', 'title' => theme_config('feature_two_title'), 'text' => theme_config('feature_two_text')],
                    ['icon' => 'bi-headset', 'title' => theme_config('feature_three_title'), 'text' => theme_config('feature_three_text')],
                ] as $feature)
                    <div class="col-lg-4">
                        <div class="mf-card">
                            <div class="mf-card-icon"><i class="bi {{ $feature['icon'] }}"></i></div>
                            <h3>{{ $feature['title'] }}</h3>
                            <p class="mf-muted mb-0">{{ $feature['text'] }}</p>
                        </div>
                    </div>
                @endforeach
            </div>
        </div>
    </section>

    <section class="container">
        <div class="mf-section">
            <div class="mf-section-title">
                <span class="mf-eyebrow">{{ trans('theme::messages.sections.integrations') }}</span>
                <h2 class="mt-3 mb-2">SkinAPI + Shop + FAQ</h2>
                <p class="mf-muted mb-0">Три ключевых плагина встроены в общий стиль темы и связаны навигацией, CTA и карточками сценариев.</p>
            </div>

            <div class="mf-plugin-grid">
                <article class="mf-plugin-card">
                    <div class="mf-plugin-icon"><i class="bi bi-person-badge"></i></div>
                    <h3>{{ trans('theme::messages.skin.title') }}</h3>
                    <p class="mf-muted">{{ theme_config('skin_intro') }}</p>
                    <ul class="mf-muted">
                        <li>{{ trans('theme::messages.skin.upload') }}</li>
                        <li>{{ trans('theme::messages.skin.cape') }}</li>
                        <li>{{ trans('theme::messages.skin.sync') }}</li>
                    </ul>
                    <div class="mf-plugin-actions">
                        <a class="mf-btn-primary" href="{{ url('/skin-api') }}">{{ trans('theme::messages.nav.skins') }}</a>
                    </div>
                </article>

                <article class="mf-plugin-card">
                    <div class="mf-plugin-icon"><i class="bi bi-cart3"></i></div>
                    <h3>{{ trans('theme::messages.shop.title') }}</h3>
                    <p class="mf-muted">{{ theme_config('shop_intro') }}</p>
                    <div class="mf-price mb-3">{{ trans('theme::messages.shop.starting_at') }} 99₽</div>
                    <div class="mf-plugin-actions">
                        <a class="mf-btn-primary" href="{{ url('/shop') }}">{{ trans('theme::messages.nav.shop') }}</a>
                    </div>
                </article>

                <article class="mf-plugin-card">
                    <div class="mf-plugin-icon"><i class="bi bi-patch-question"></i></div>
                    <h3>{{ trans('theme::messages.faq.title') }}</h3>
                    <p class="mf-muted">{{ theme_config('faq_intro') }}</p>
                    <div class="mf-plugin-actions">
                        <a class="mf-btn-primary" href="{{ url('/faq') }}">{{ trans('theme::messages.nav.faq') }}</a>
                    </div>
                </article>
            </div>
        </div>
    </section>
@endsection
