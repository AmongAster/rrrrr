<footer class="mf-footer mt-5">
    <div class="container">
        <div class="row g-4 align-items-center">
            <div class="col-lg-7">
                <a class="mf-brand mb-3" href="{{ url('/') }}">
                    <span class="mf-brand-mark"><i class="bi bi-hexagon-fill"></i></span>
                    <span>{{ trans('theme::messages.brand') }}</span>
                </a>
                <p class="mf-muted mb-0">{{ trans('theme::messages.footer.rights') }}</p>
            </div>
            <div class="col-lg-5 text-lg-end">
                @if (theme_config('support_email'))
                    <a class="mf-btn-secondary" href="mailto:{{ theme_config('support_email') }}">
                        <i class="bi bi-envelope"></i>
                        {{ trans('theme::messages.footer.contact') }}
                    </a>
                @endif
            </div>
        </div>
    </div>
</footer>
