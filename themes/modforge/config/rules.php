<?php

return [
    'accent_color' => ['required', 'string'],
    'hero_server_ip' => ['required', 'string', 'max:255'],
    'hero_server_version' => ['required', 'string', 'max:255'],
    'discord_url' => ['nullable', 'url', 'max:255'],
    'support_email' => ['nullable', 'email', 'max:255'],
    'cta_primary_label' => ['required', 'string', 'max:80'],
    'cta_primary_url' => ['required', 'string', 'max:255'],
    'cta_secondary_label' => ['required', 'string', 'max:80'],
    'cta_secondary_url' => ['required', 'string', 'max:255'],
    'feature_one_title' => ['required', 'string', 'max:100'],
    'feature_one_text' => ['required', 'string', 'max:255'],
    'feature_two_title' => ['required', 'string', 'max:100'],
    'feature_two_text' => ['required', 'string', 'max:255'],
    'feature_three_title' => ['required', 'string', 'max:100'],
    'feature_three_text' => ['required', 'string', 'max:255'],
    'faq_intro' => ['required', 'string', 'max:255'],
    'shop_intro' => ['required', 'string', 'max:255'],
    'skin_intro' => ['required', 'string', 'max:255'],
    'launcher_download_url' => ['nullable', 'url', 'max:255'],
];
