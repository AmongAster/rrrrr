<form action="{{ route('admin.themes.update', $theme) }}" method="POST" class="row g-4">
    @csrf

    <div class="col-12">
        <h4>{{ trans('theme::messages.admin.branding') }}</h4>
    </div>

    <div class="col-md-4">
        <label class="form-label" for="accentColor">{{ trans('theme::messages.admin.accent_color') }}</label>
        <input id="accentColor" type="text" name="accent_color" class="form-control @error('accent_color') is-invalid @enderror" value="{{ old('accent_color', theme_config('accent_color')) }}">
        @error('accent_color')<div class="invalid-feedback">{{ $message }}</div>@enderror
    </div>

    <div class="col-md-4">
        <label class="form-label" for="serverIp">{{ trans('theme::messages.admin.server_ip') }}</label>
        <input id="serverIp" type="text" name="hero_server_ip" class="form-control @error('hero_server_ip') is-invalid @enderror" value="{{ old('hero_server_ip', theme_config('hero_server_ip')) }}">
        @error('hero_server_ip')<div class="invalid-feedback">{{ $message }}</div>@enderror
    </div>

    <div class="col-md-4">
        <label class="form-label" for="serverVersion">{{ trans('theme::messages.admin.server_version') }}</label>
        <input id="serverVersion" type="text" name="hero_server_version" class="form-control @error('hero_server_version') is-invalid @enderror" value="{{ old('hero_server_version', theme_config('hero_server_version')) }}">
        @error('hero_server_version')<div class="invalid-feedback">{{ $message }}</div>@enderror
    </div>

    <div class="col-md-6">
        <label class="form-label" for="discordUrl">Discord URL</label>
        <input id="discordUrl" type="url" name="discord_url" class="form-control @error('discord_url') is-invalid @enderror" value="{{ old('discord_url', theme_config('discord_url')) }}">
        @error('discord_url')<div class="invalid-feedback">{{ $message }}</div>@enderror
    </div>

    <div class="col-md-6">
        <label class="form-label" for="supportEmail">Support email</label>
        <input id="supportEmail" type="email" name="support_email" class="form-control @error('support_email') is-invalid @enderror" value="{{ old('support_email', theme_config('support_email')) }}">
        @error('support_email')<div class="invalid-feedback">{{ $message }}</div>@enderror
    </div>

    <div class="col-12">
        <h4>{{ trans('theme::messages.admin.hero_buttons') }}</h4>
    </div>

    <div class="col-md-3">
        <label class="form-label" for="ctaPrimaryLabel">{{ trans('theme::messages.admin.primary_label') }}</label>
        <input id="ctaPrimaryLabel" type="text" name="cta_primary_label" class="form-control @error('cta_primary_label') is-invalid @enderror" value="{{ old('cta_primary_label', theme_config('cta_primary_label')) }}">
        @error('cta_primary_label')<div class="invalid-feedback">{{ $message }}</div>@enderror
    </div>

    <div class="col-md-3">
        <label class="form-label" for="ctaPrimaryUrl">{{ trans('theme::messages.admin.primary_url') }}</label>
        <input id="ctaPrimaryUrl" type="text" name="cta_primary_url" class="form-control @error('cta_primary_url') is-invalid @enderror" value="{{ old('cta_primary_url', theme_config('cta_primary_url')) }}">
        @error('cta_primary_url')<div class="invalid-feedback">{{ $message }}</div>@enderror
    </div>

    <div class="col-md-3">
        <label class="form-label" for="ctaSecondaryLabel">{{ trans('theme::messages.admin.secondary_label') }}</label>
        <input id="ctaSecondaryLabel" type="text" name="cta_secondary_label" class="form-control @error('cta_secondary_label') is-invalid @enderror" value="{{ old('cta_secondary_label', theme_config('cta_secondary_label')) }}">
        @error('cta_secondary_label')<div class="invalid-feedback">{{ $message }}</div>@enderror
    </div>

    <div class="col-md-3">
        <label class="form-label" for="ctaSecondaryUrl">{{ trans('theme::messages.admin.secondary_url') }}</label>
        <input id="ctaSecondaryUrl" type="text" name="cta_secondary_url" class="form-control @error('cta_secondary_url') is-invalid @enderror" value="{{ old('cta_secondary_url', theme_config('cta_secondary_url')) }}">
        @error('cta_secondary_url')<div class="invalid-feedback">{{ $message }}</div>@enderror
    </div>

    @foreach ([1, 2, 3] as $feature)
        <div class="col-md-4">
            <label class="form-label" for="featureTitle{{ $feature }}">{{ trans('theme::messages.admin.feature_title') }} #{{ $feature }}</label>
            <input id="featureTitle{{ $feature }}" type="text" name="feature_{{ ['one', 'two', 'three'][$feature - 1] }}_title" class="form-control @error('feature_'.['one', 'two', 'three'][$feature - 1].'_title') is-invalid @enderror" value="{{ old('feature_'.['one', 'two', 'three'][$feature - 1].'_title', theme_config('feature_'.['one', 'two', 'three'][$feature - 1].'_title')) }}">
            @error('feature_'.['one', 'two', 'three'][$feature - 1].'_title')<div class="invalid-feedback">{{ $message }}</div>@enderror
        </div>
        <div class="col-md-8">
            <label class="form-label" for="featureText{{ $feature }}">{{ trans('theme::messages.admin.feature_text') }} #{{ $feature }}</label>
            <input id="featureText{{ $feature }}" type="text" name="feature_{{ ['one', 'two', 'three'][$feature - 1] }}_text" class="form-control @error('feature_'.['one', 'two', 'three'][$feature - 1].'_text') is-invalid @enderror" value="{{ old('feature_'.['one', 'two', 'three'][$feature - 1].'_text', theme_config('feature_'.['one', 'two', 'three'][$feature - 1].'_text')) }}">
            @error('feature_'.['one', 'two', 'three'][$feature - 1].'_text')<div class="invalid-feedback">{{ $message }}</div>@enderror
        </div>
    @endforeach

    <div class="col-md-4">
        <label class="form-label" for="skinIntro">{{ trans('theme::messages.admin.skin_intro') }}</label>
        <input id="skinIntro" type="text" name="skin_intro" class="form-control @error('skin_intro') is-invalid @enderror" value="{{ old('skin_intro', theme_config('skin_intro')) }}">
        @error('skin_intro')<div class="invalid-feedback">{{ $message }}</div>@enderror
    </div>

    <div class="col-md-4">
        <label class="form-label" for="shopIntro">{{ trans('theme::messages.admin.shop_intro') }}</label>
        <input id="shopIntro" type="text" name="shop_intro" class="form-control @error('shop_intro') is-invalid @enderror" value="{{ old('shop_intro', theme_config('shop_intro')) }}">
        @error('shop_intro')<div class="invalid-feedback">{{ $message }}</div>@enderror
    </div>

    <div class="col-md-4">
        <label class="form-label" for="faqIntro">{{ trans('theme::messages.admin.faq_intro') }}</label>
        <input id="faqIntro" type="text" name="faq_intro" class="form-control @error('faq_intro') is-invalid @enderror" value="{{ old('faq_intro', theme_config('faq_intro')) }}">
        @error('faq_intro')<div class="invalid-feedback">{{ $message }}</div>@enderror
    </div>

    <div class="col-md-6">
        <label class="form-label" for="launcherUrl">{{ trans('theme::messages.admin.launcher_url') }}</label>
        <input id="launcherUrl" type="url" name="launcher_download_url" class="form-control @error('launcher_download_url') is-invalid @enderror" value="{{ old('launcher_download_url', theme_config('launcher_download_url')) }}">
        @error('launcher_download_url')<div class="invalid-feedback">{{ $message }}</div>@enderror
    </div>

    <div class="col-12">
        <button type="submit" class="btn btn-primary">
            <i class="bi bi-save"></i> {{ trans('messages.actions.save') }}
        </button>
    </div>
</form>
