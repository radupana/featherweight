export const SITE_CONFIG = {
  title: 'Featherweight - Intelligent Weightlifting Tracker',
  description: 'Track your lifts, crush your goals. AI-powered workout parsing, voice input, and training insights. Free, offline-first, no ads.',
  url: 'https://featherweight.app',
  author: 'Featherweight',

  socials: {
    github: 'https://github.com/radupana/featherweight',
  },

  navLinks: [
    { href: '#features', label: 'Features' },
    { href: '#how-it-works', label: 'How It Works' },
    { href: '#faq', label: 'FAQ' },
  ],

  features: [
    {
      title: 'AI Programme Import',
      description: 'Paste any training programme in plain text and AI converts it to structured workouts with exercises, sets, reps, and weights.',
      icon: 'sparkles',
    },
    {
      title: 'Voice Input',
      description: 'Log exercises hands-free. Just speak your sets and reps - AI transcribes and parses your workout automatically.',
      icon: 'microphone',
    },
    {
      title: 'AI Training Insights',
      description: 'Get coaching feedback on your training history - volume trends, progression analysis, and personalized recommendations.',
      icon: 'light-bulb',
    },
    {
      title: '1RM & PR Tracking',
      description: 'Automatic 1RM calculations using multiple formulas. See real-time % of 1RM during workouts. PR detection with celebration.',
      icon: 'trophy',
    },
    {
      title: 'Programme Management',
      description: 'Multi-week structured programmes with auto-progression, deload rules, and deviation tracking when you swap exercises.',
      icon: 'clipboard-list',
    },
    {
      title: 'Offline-First',
      description: 'Works without internet. All data stored locally. Optional cloud sync keeps devices in sync when connected.',
      icon: 'wifi-off',
    },
  ],

  faqs: [
    {
      question: 'Is Featherweight free?',
      answer: 'Yes, completely free with no ads or in-app purchases.',
    },
    {
      question: 'What AI features are included?',
      answer: 'AI programme parsing (import any text-based programme), voice transcription for hands-free logging, and training insights that analyze your workout history.',
    },
    {
      question: 'Does it work offline?',
      answer: 'Yes! Featherweight is offline-first. All workouts are stored locally on your device and sync when you have internet.',
    },
    {
      question: 'How does voice input work?',
      answer: 'Tap the microphone, speak your exercises and sets naturally (e.g., "bench press 3 sets of 8 at 185 pounds"), and AI parses it into structured data.',
    },
    {
      question: 'What exercises are supported?',
      answer: '300+ exercises are built-in with muscle groups and equipment tags. You can also create unlimited custom exercises.',
    },
    {
      question: 'How is 1RM calculated?',
      answer: 'Using the Brzycki formula with RPE adjustments. The app tracks your estimated 1RM over time and shows % of 1RM in real-time during workouts.',
    },
    {
      question: 'Is it available on iOS?',
      answer: 'Android first. iOS is planned for the future based on demand.',
    },
    {
      question: 'When will it be released?',
      answer: 'Coming soon to Google Play Store!',
    },
  ],
} as const;

export type Feature = typeof SITE_CONFIG.features[number];
export type FAQ = typeof SITE_CONFIG.faqs[number];
export type NavLink = typeof SITE_CONFIG.navLinks[number];
