@extends('layouts.app')

@section('title', trans('theme::messages.skin.title'))
@section('page-title', trans('theme::messages.skin.title'))
@section('page-description', theme_config('skin_intro'))

@section('content')
    <div class="row g-4">
        <div class="col-lg-8">
            <div class="mf-section h-100">
                <span class="mf-eyebrow">SkinAPI</span>
                <h2 class="mt-3">{{ trans('theme::messages.skin.description') }}</h2>
                <p class="mf-muted">{{ theme_config('skin_intro') }}</p>

                <div class="row g-4 mt-1">
                    <div class="col-md-4">
                        <div class="mf-card">
                            <div class="mf-card-icon"><i class="bi bi-upload"></i></div>
                            <h4>{{ trans('theme::messages.skin.upload') }}</h4>
                            <p class="mf-muted mb-0">PNG-скины и плащи в едином визуальном стиле для сайта и лаунчера.</p>
                        </div>
                    </div>
                    <div class="col-md-4">
                        <div class="mf-card">
                            <div class="mf-card-icon"><i class="bi bi-images"></i></div>
                            <h4>{{ trans('theme::messages.skin.cape') }}</h4>
                            <p class="mf-muted mb-0">Витрина с предпросмотром образа игрока и быстрым доступом к текущему комплекту.</p>
                        </div>
                    </div>
                    <div class="col-md-4">
                        <div class="mf-card">
                            <div class="mf-card-icon"><i class="bi bi-arrow-repeat"></i></div>
                            <h4>{{ trans('theme::messages.skin.sync') }}</h4>
                            <p class="mf-muted mb-0">Объяснение процесса синхронизации профиля с модовым клиентом и сайтом.</p>
                        </div>
                    </div>
                </div>
            </div>
        </div>
        <div class="col-lg-4">
            <div class="mf-panel h-100">
                <span class="mf-badge mb-3"><i class="bi bi-person-circle"></i> Player identity</span>
                <h3>Быстрые действия</h3>
                <p class="mf-muted">Используйте этот блок как обертку вокруг стандартного контента плагина или как side panel с подсказками.</p>
                <div class="mf-stack flex-column align-items-stretch mt-4">
                    <a class="mf-btn-primary" href="#upload-form">{{ trans('theme::messages.skin.upload') }}</a>
                    <a class="mf-btn-secondary" href="{{ url('/faq') }}">Открыть FAQ по скинам</a>
                </div>
            </div>
        </div>
    </div>

    <div class="mf-section mt-4" id="upload-form">
        @yield('skinapi-content')
    </div>
@endsection
