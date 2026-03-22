@extends('layouts.app')

@section('title', trans('theme::messages.faq.title'))
@section('page-title', trans('theme::messages.faq.title'))
@section('page-description', theme_config('faq_intro'))

@section('content')
    <div class="mf-section">
        <div class="row g-4 align-items-start">
            <div class="col-lg-4">
                <div class="mf-panel h-100">
                    <span class="mf-eyebrow">FAQ</span>
                    <h3 class="mt-3">{{ trans('theme::messages.faq.description') }}</h3>
                    <p class="mf-muted">{{ theme_config('faq_intro') }}</p>
                    <input type="search" class="mf-search mt-3" placeholder="{{ trans('theme::messages.faq.search') }}">
                </div>
            </div>
            <div class="col-lg-8">
                <div class="mf-card mf-faq-item">
                    <h4>Как установить модпак?</h4>
                    <p class="mf-muted mb-0">Добавьте ссылку на лаунчер, системные требования и шаги установки прямо в верхние ответы FAQ.</p>
                </div>
                <div class="mf-card mf-faq-item">
                    <h4>Как загрузить скин?</h4>
                    <p class="mf-muted mb-0">Свяжите ответ с маршрутом SkinAPI и объясните лимиты по разрешению, формату и времени обновления.</p>
                </div>
                <div class="mf-card mf-faq-item">
                    <h4>Что делать, если покупка не пришла?</h4>
                    <p class="mf-muted mb-0">Подскажите игроку проверить платеж, инвентарь, историю магазина и канал поддержки.</p>
                </div>
            </div>
        </div>
    </div>

    <div class="mf-section mt-4">
        @yield('faq-content')
    </div>
@endsection
