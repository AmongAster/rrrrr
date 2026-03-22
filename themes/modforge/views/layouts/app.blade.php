@extends('layouts.base')

@section('app-content')
    <section class="mf-page-hero container py-5 mt-5">
        <div class="mf-section">
            <div class="row align-items-center g-4">
                <div class="col-lg-8">
                    <span class="mf-eyebrow">{{ trans('theme::messages.sections.integrations') }}</span>
                    <h1>@yield('page-title', trans('theme::messages.brand'))</h1>
                    <p class="mf-muted mb-0">@yield('page-description', trans('theme::messages.hero.description'))</p>
                </div>
                <div class="col-lg-4 text-lg-end">
                    <span class="mf-badge"><i class="bi bi-stars"></i> {{ theme_config('hero_server_version') }}</span>
                </div>
            </div>
        </div>
    </section>

    <section class="container pb-5">
        @yield('content')
    </section>
@endsection
