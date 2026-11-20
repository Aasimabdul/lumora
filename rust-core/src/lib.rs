//! Lumora core: pure, deterministic helpers for theme configuration.
//!
//! Two responsibilities live here:
//!
//! 1. **Contrast maths** — WCAG 2.x relative luminance and contrast ratio, plus
//!    [`readable_foreground`], which nudges a text colour until it is readable
//!    against a background.
//! 2. **Theme configuration validation** — [`ThemeConfig::validate`] reports the
//!    first problem in a user-created theme, and [`ThemeConfig::sanitize`]
//!    repairs a corrupted configuration so the app can always render something.
//!
//! The crate has no dependencies and no `unsafe` code. It is **not yet linked
//! into the Android app** (no NDK is available in the current build
//! environment); see `docs/ARCHITECTURE.md`.

use std::fmt;

/// Errors produced by this crate.
#[derive(Debug, Clone, PartialEq)]
pub enum CoreError {
    /// A colour string was not of the form `#RRGGBB`.
    InvalidColor(String),
    /// A numeric field was outside its permitted range.
    OutOfRange {
        /// Name of the offending field.
        field: &'static str,
        /// The rejected value.
        value: f32,
        /// Inclusive lower bound.
        min: f32,
        /// Inclusive upper bound.
        max: f32,
    },
    /// A numeric field was NaN or infinite.
    NotFinite {
        /// Name of the offending field.
        field: &'static str,
    },
    /// The requested contrast ratio is outside `1.0..=21.0`.
    InvalidContrastTarget(f32),
    /// No foreground adjustment can reach the requested contrast.
    ContrastUnattainable {
        /// The best ratio reachable against this background.
        best: f64,
    },
}

impl fmt::Display for CoreError {
    fn fmt(&self, f: &mut fmt::Formatter<'_>) -> fmt::Result {
        match self {
            Self::InvalidColor(s) => write!(f, "invalid colour {s:?}, expected #RRGGBB"),
            Self::OutOfRange {
                field,
                value,
                min,
                max,
            } => write!(f, "{field} = {value} is outside {min}..={max}"),
            Self::NotFinite { field } => write!(f, "{field} is not a finite number"),
            Self::InvalidContrastTarget(v) => {
                write!(f, "contrast target {v} is outside 1.0..=21.0")
            }
            Self::ContrastUnattainable { best } => {
                write!(f, "requested contrast is unattainable (best {best:.2}:1)")
            }
        }
    }
}

impl std::error::Error for CoreError {}

/// An opaque sRGB colour.
#[derive(Debug, Clone, Copy, PartialEq, Eq)]
pub struct Rgb {
    /// Red channel.
    pub r: u8,
    /// Green channel.
    pub g: u8,
    /// Blue channel.
    pub b: u8,
}

impl Rgb {
    /// Pure black, `#000000`.
    pub const BLACK: Rgb = Rgb { r: 0, g: 0, b: 0 };
    /// Pure white, `#FFFFFF`.
    pub const WHITE: Rgb = Rgb {
        r: 255,
        g: 255,
        b: 255,
    };

    /// Parses `#RRGGBB` (case-insensitive). Shorthand and alpha forms are rejected.
    pub fn from_hex(s: &str) -> Result<Rgb, CoreError> {
        let err = || CoreError::InvalidColor(s.to_owned());
        let digits = s.strip_prefix('#').ok_or_else(err)?;
        if digits.len() != 6 || !digits.bytes().all(|b| b.is_ascii_hexdigit()) {
            return Err(err());
        }
        let channel = |i: usize| u8::from_str_radix(&digits[i..i + 2], 16).map_err(|_| err());
        Ok(Rgb {
            r: channel(0)?,
            g: channel(2)?,
            b: channel(4)?,
        })
    }

    /// Formats as upper-case `#RRGGBB`.
    pub fn to_hex(self) -> String {
        format!("#{:02X}{:02X}{:02X}", self.r, self.g, self.b)
    }

    /// WCAG 2.x relative luminance in `0.0..=1.0`.
    pub fn relative_luminance(self) -> f64 {
        fn linear(c: u8) -> f64 {
            let c = f64::from(c) / 255.0;
            if c <= 0.039_28 {
                c / 12.92
            } else {
                ((c + 0.055) / 1.055).powf(2.4)
            }
        }
        0.2126 * linear(self.r) + 0.7152 * linear(self.g) + 0.0722 * linear(self.b)
    }

    fn lerp(self, to: Rgb, t: f64) -> Rgb {
        let mix = |a: u8, b: u8| {
            let v = f64::from(a) + (f64::from(b) - f64::from(a)) * t;
            v.round().clamp(0.0, 255.0) as u8
        };
        Rgb {
            r: mix(self.r, to.r),
            g: mix(self.g, to.g),
            b: mix(self.b, to.b),
        }
    }
}

/// WCAG contrast ratio between two colours, in `1.0..=21.0`. Symmetric.
pub fn contrast_ratio(a: Rgb, b: Rgb) -> f64 {
    let (la, lb) = (a.relative_luminance(), b.relative_luminance());
    let (hi, lo) = if la >= lb { (la, lb) } else { (lb, la) };
    (hi + 0.05) / (lo + 0.05)
}

/// Returns `fg` unchanged if it already meets `min_ratio` against `bg`;
/// otherwise blends it towards white or black (whichever contrasts more with
/// `bg`) in 1 % steps until the ratio is met, keeping as much of the original
/// hue as possible.
///
/// # Errors
///
/// * [`CoreError::InvalidContrastTarget`] if `min_ratio` is not in `1.0..=21.0`.
/// * [`CoreError::ContrastUnattainable`] if even pure black/white cannot reach it.
pub fn readable_foreground(fg: Rgb, bg: Rgb, min_ratio: f32) -> Result<Rgb, CoreError> {
    if !min_ratio.is_finite() || !(1.0..=21.0).contains(&min_ratio) {
        return Err(CoreError::InvalidContrastTarget(min_ratio));
    }
    let target = f64::from(min_ratio);
    if contrast_ratio(fg, bg) >= target {
        return Ok(fg);
    }
    let pole = if contrast_ratio(Rgb::WHITE, bg) >= contrast_ratio(Rgb::BLACK, bg) {
        Rgb::WHITE
    } else {
        Rgb::BLACK
    };
    let best = contrast_ratio(pole, bg);
    if best < target {
        return Err(CoreError::ContrastUnattainable { best });
    }
    for step in 1..=100 {
        let candidate = fg.lerp(pole, f64::from(step) / 100.0);
        if contrast_ratio(candidate, bg) >= target {
            return Ok(candidate);
        }
    }
    Ok(pole)
}

/// A user-editable theme configuration (subset shared with the Android app).
#[derive(Debug, Clone, PartialEq)]
pub struct ThemeConfig {
    /// Clock size multiplier, `0.4..=2.5`.
    pub clock_scale: f32,
    /// Horizontal clock position as a fraction of width, `0.0..=1.0`.
    pub position_x: f32,
    /// Vertical clock position as a fraction of height, `0.0..=1.0`.
    pub position_y: f32,
    /// Background dimming, `0.0..=1.0`.
    pub dim: f32,
    /// Background blur radius in dp, `0.0..=25.0`.
    pub blur_radius: f32,
    /// Text colour, `#RRGGBB`.
    pub text_color: String,
    /// Background colour, `#RRGGBB`.
    pub background_color: String,
}

impl Default for ThemeConfig {
    fn default() -> Self {
        Self {
            clock_scale: 1.0,
            position_x: 0.5,
            position_y: 0.5,
            dim: 0.0,
            blur_radius: 0.0,
            text_color: "#FFFFFF".to_owned(),
            background_color: "#000000".to_owned(),
        }
    }
}

fn check_range(field: &'static str, value: f32, min: f32, max: f32) -> Result<(), CoreError> {
    if !value.is_finite() {
        Err(CoreError::NotFinite { field })
    } else if value < min || value > max {
        Err(CoreError::OutOfRange {
            field,
            value,
            min,
            max,
        })
    } else {
        Ok(())
    }
}

fn repair(value: f32, default: f32, min: f32, max: f32) -> f32 {
    if value.is_finite() {
        value.clamp(min, max)
    } else {
        default
    }
}

impl ThemeConfig {
    /// Permitted clock scale range.
    pub const SCALE_RANGE: (f32, f32) = (0.4, 2.5);
    /// Permitted blur radius range in dp.
    pub const BLUR_RANGE: (f32, f32) = (0.0, 25.0);

    /// Returns the first problem found, or `Ok(())` if the configuration is valid.
    pub fn validate(&self) -> Result<(), CoreError> {
        let (smin, smax) = Self::SCALE_RANGE;
        let (bmin, bmax) = Self::BLUR_RANGE;
        check_range("clock_scale", self.clock_scale, smin, smax)?;
        check_range("position_x", self.position_x, 0.0, 1.0)?;
        check_range("position_y", self.position_y, 0.0, 1.0)?;
        check_range("dim", self.dim, 0.0, 1.0)?;
        check_range("blur_radius", self.blur_radius, bmin, bmax)?;
        Rgb::from_hex(&self.text_color)?;
        Rgb::from_hex(&self.background_color)?;
        Ok(())
    }

    /// Contrast ratio between the text and background colours.
    pub fn text_contrast(&self) -> Result<f64, CoreError> {
        Ok(contrast_ratio(
            Rgb::from_hex(&self.text_color)?,
            Rgb::from_hex(&self.background_color)?,
        ))
    }

    /// Returns a valid copy: out-of-range numbers are clamped, non-finite
    /// numbers and malformed colours fall back to the defaults. The result
    /// always passes [`ThemeConfig::validate`].
    pub fn sanitize(&self) -> ThemeConfig {
        let d = ThemeConfig::default();
        let (smin, smax) = Self::SCALE_RANGE;
        let (bmin, bmax) = Self::BLUR_RANGE;
        let colour = |value: &str, default: &str| {
            Rgb::from_hex(value).map_or_else(|_| default.to_owned(), Rgb::to_hex)
        };
        ThemeConfig {
            clock_scale: repair(self.clock_scale, d.clock_scale, smin, smax),
            position_x: repair(self.position_x, d.position_x, 0.0, 1.0),
            position_y: repair(self.position_y, d.position_y, 0.0, 1.0),
            dim: repair(self.dim, d.dim, 0.0, 1.0),
            blur_radius: repair(self.blur_radius, d.blur_radius, bmin, bmax),
            text_color: colour(&self.text_color, &d.text_color),
            background_color: colour(&self.background_color, &d.background_color),
        }
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    fn rgb(hex: &str) -> Rgb {
        Rgb::from_hex(hex).expect("valid test colour")
    }

    #[test]
    fn hex_round_trip_and_case_insensitivity() {
        assert_eq!(
            rgb("#00e5ff"),
            Rgb {
                r: 0,
                g: 229,
                b: 255
            }
        );
        assert_eq!(rgb("#00e5ff").to_hex(), "#00E5FF");
    }

    #[test]
    fn hex_rejects_malformed_input() {
        for bad in ["", "#", "FFFFFF", "#FFF", "#FFFFFFFF", "#GGGGGG", "#12345é"] {
            assert_eq!(
                Rgb::from_hex(bad),
                Err(CoreError::InvalidColor(bad.to_owned())),
                "{bad:?}"
            );
        }
    }

    #[test]
    fn black_on_white_is_21_to_1() {
        let ratio = contrast_ratio(Rgb::BLACK, Rgb::WHITE);
        assert!((ratio - 21.0).abs() < 1e-9, "{ratio}");
    }

    #[test]
    fn identical_colours_are_1_to_1_and_ratio_is_symmetric() {
        assert!((contrast_ratio(rgb("#336699"), rgb("#336699")) - 1.0).abs() < 1e-12);
        let (a, b) = (rgb("#123456"), rgb("#ABCDEF"));
        assert_eq!(contrast_ratio(a, b), contrast_ratio(b, a));
    }

    #[test]
    fn mid_grey_on_white_matches_published_value() {
        // #777777 on white is the well-known ~4.48:1 case.
        let ratio = contrast_ratio(rgb("#777777"), Rgb::WHITE);
        assert!((ratio - 4.48).abs() < 0.02, "{ratio}");
    }

    #[test]
    fn readable_foreground_keeps_colour_that_already_passes() {
        let fg = rgb("#00E5FF");
        assert_eq!(readable_foreground(fg, Rgb::BLACK, 4.5), Ok(fg));
    }

    #[test]
    fn readable_foreground_lightens_dark_text_on_amoled_black() {
        let out = readable_foreground(rgb("#202020"), Rgb::BLACK, 4.5).unwrap();
        assert!(contrast_ratio(out, Rgb::BLACK) >= 4.5);
        assert!(out.r > 0x20, "should have moved toward white: {out:?}");
    }

    #[test]
    fn readable_foreground_darkens_light_text_on_white() {
        let out = readable_foreground(rgb("#DDDDDD"), Rgb::WHITE, 7.0).unwrap();
        assert!(contrast_ratio(out, Rgb::WHITE) >= 7.0);
    }

    #[test]
    fn readable_foreground_reports_unattainable_targets() {
        // Mid-grey background: neither pole reaches 7:1.
        match readable_foreground(rgb("#777777"), rgb("#777777"), 7.0) {
            Err(CoreError::ContrastUnattainable { best }) => assert!(best < 7.0),
            other => panic!("unexpected {other:?}"),
        }
    }

    #[test]
    fn readable_foreground_rejects_bad_targets() {
        for bad in [0.5_f32, 21.5, f32::NAN, f32::INFINITY] {
            assert!(matches!(
                readable_foreground(Rgb::WHITE, Rgb::BLACK, bad),
                Err(CoreError::InvalidContrastTarget(_))
            ));
        }
    }

    #[test]
    fn default_config_is_valid_and_high_contrast() {
        let c = ThemeConfig::default();
        assert_eq!(c.validate(), Ok(()));
        assert!((c.text_contrast().unwrap() - 21.0).abs() < 1e-9);
    }

    #[test]
    fn validate_reports_each_kind_of_problem() {
        let base = ThemeConfig::default();
        let cases: Vec<(ThemeConfig, &str)> = vec![
            (
                ThemeConfig {
                    clock_scale: 3.0,
                    ..base.clone()
                },
                "clock_scale",
            ),
            (
                ThemeConfig {
                    clock_scale: 0.1,
                    ..base.clone()
                },
                "clock_scale",
            ),
            (
                ThemeConfig {
                    position_x: -0.1,
                    ..base.clone()
                },
                "position_x",
            ),
            (
                ThemeConfig {
                    position_y: 1.1,
                    ..base.clone()
                },
                "position_y",
            ),
            (
                ThemeConfig {
                    dim: 2.0,
                    ..base.clone()
                },
                "dim",
            ),
            (
                ThemeConfig {
                    blur_radius: 26.0,
                    ..base.clone()
                },
                "blur_radius",
            ),
        ];
        for (cfg, field) in cases {
            match cfg.validate() {
                Err(CoreError::OutOfRange { field: f, .. }) => assert_eq!(f, field),
                other => panic!("{field}: unexpected {other:?}"),
            }
        }
        let nan = ThemeConfig {
            dim: f32::NAN,
            ..base.clone()
        };
        assert_eq!(nan.validate(), Err(CoreError::NotFinite { field: "dim" }));
        let bad_colour = ThemeConfig {
            text_color: "white".into(),
            ..base
        };
        assert!(matches!(
            bad_colour.validate(),
            Err(CoreError::InvalidColor(_))
        ));
    }

    #[test]
    fn range_boundaries_are_inclusive() {
        let c = ThemeConfig {
            clock_scale: 2.5,
            position_x: 0.0,
            position_y: 1.0,
            dim: 1.0,
            blur_radius: 25.0,
            ..ThemeConfig::default()
        };
        assert_eq!(c.validate(), Ok(()));
    }

    #[test]
    fn sanitize_repairs_corrupted_configuration() {
        let corrupted = ThemeConfig {
            clock_scale: 99.0,
            position_x: f32::NAN,
            position_y: -5.0,
            dim: f32::INFINITY,
            blur_radius: -1.0,
            text_color: "not-a-colour".into(),
            background_color: "#0a0a0a".into(),
        };
        let fixed = corrupted.sanitize();
        assert_eq!(fixed.validate(), Ok(()));
        assert_eq!(fixed.clock_scale, 2.5);
        assert_eq!(fixed.position_x, 0.5);
        assert_eq!(fixed.position_y, 0.0);
        assert_eq!(fixed.dim, 0.0);
        assert_eq!(fixed.blur_radius, 0.0);
        assert_eq!(fixed.text_color, "#FFFFFF");
        assert_eq!(fixed.background_color, "#0A0A0A");
    }

    #[test]
    fn sanitize_is_idempotent_and_leaves_valid_config_alone() {
        let c = ThemeConfig {
            clock_scale: 1.5,
            text_color: "#00E5FF".into(),
            ..ThemeConfig::default()
        };
        assert_eq!(c.sanitize(), c);
        assert_eq!(c.sanitize().sanitize(), c.sanitize());
    }
}
