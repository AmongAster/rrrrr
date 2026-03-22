@extends('layouts.app')

@section('title', trans('theme::messages.shop.title'))
@section('page-title', trans('theme::messages.shop.title'))
@section('page-description', theme_config('shop_intro'))

@section('content')
    <div class="mf-section">
        <div class="d-flex justify-content-between align-items-end flex-wrap gap-3 mf-section-title">
            <div>
                <span class="mf-eyebrow">Shop</span>
                <h2 class="mt-3 mb-2">{{ trans('theme::messages.shop.description') }}</h2>
                <p class="mf-muted mb-0">{{ theme_config('shop_intro') }}</p>
            </div>
            <div class="mf-badge"><i class="bi bi-shield-lock"></i> Secure checkout</div>
        </div>

        <div class="row g-4">
            <div class="col-lg-4">
                <div class="mf-plugin-card h-100">
                    <div class="mf-plugin-icon"><i class="bi bi-stars"></i></div>
                    <div class="text-uppercase small mf-muted mb-2">{{ trans('theme::messages.shop.featured') }}</div>
                    <h3>Void Engineer</h3>
                    <p class="mf-muted">Набор с косметикой, бустерами и доступом к сезонным испытаниям.</p>
                    <div class="mf-price">499₽</div>
                </div>
            </div>
            <div class="col-lg-8">
                <div class="mf-panel h-100">
                    <h3 class="mb-3">Рекомендации по структуре магазина</h3>
                    <ul class="mf-muted mb-0">
                        <li>Выводите топовые категории и акции в верхнем блоке страницы магазина.</li>
                        <li>Используйте FAQ-ссылки рядом с оплатой и возвратами для снижения нагрузки на саппорт.</li>
                        <li>Показывайте бонусы, связанные со скинами или кастомизацией, чтобы связать Shop со SkinAPI.</li>
                    </ul>
                </div>
            </div>
        </div>
    </div>

    <div class="mf-section mt-4">
        @yield('shop-content')
    </div>
@endsection
