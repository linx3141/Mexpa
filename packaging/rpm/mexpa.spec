Name:           mexpa
Version:        1.0.0
Release:        1%{?dist}
Summary:        mihomo proxy client for Linux (Compose Desktop)
License:        GPL-3.0-only
URL:            https://github.com/linx3141/Mexpa
Source0:        Mexpa-linux-x64-%{version}.tar.gz
Source1:        mexpa.desktop
Source2:        mexpa.png
Source3:        mexpa-launcher
Source4:        LICENSE
BuildArch:      x86_64
AutoReqProv:    yes
%global debug_package %{nil}
%global __strip /bin/true
%global __os_install_post %{nil}

Requires:       glibc
Recommends:     python3-gobject
Recommends:     libayatana-appindicator-gtk3

%description
Mexpa is a mihomo-based proxy client with system proxy, TUN,
subscription management and a Compose Desktop UI.

%prep
%setup -q -c -T
tar -xzf %{SOURCE0}

%build

%install
rm -rf %{buildroot}
mkdir -p %{buildroot}/opt
cp -a mexpa %{buildroot}/opt/mexpa
install -Dm755 %{SOURCE3} %{buildroot}%{_bindir}/mexpa
install -Dm644 %{SOURCE1} %{buildroot}%{_datadir}/applications/mexpa.desktop
install -Dm644 %{SOURCE2} %{buildroot}%{_datadir}/icons/hicolor/512x512/apps/mexpa.png
install -Dm644 %{SOURCE4} %{buildroot}%{_datadir}/licenses/mexpa/LICENSE

%post
if command -v setcap >/dev/null 2>&1; then
  setcap cap_net_admin+ep /opt/mexpa/bin/mihomo || true
fi
update-desktop-database -q %{_datadir}/applications 2>/dev/null || true
gtk-update-icon-cache -q %{_datadir}/icons/hicolor 2>/dev/null || true

%postun
if command -v update-desktop-database >/dev/null 2>&1; then
  update-desktop-database -q %{_datadir}/applications || true
fi

%files
/opt/mexpa
%{_bindir}/mexpa
%{_datadir}/applications/mexpa.desktop
%{_datadir}/icons/hicolor/512x512/apps/mexpa.png
%{_datadir}/licenses/mexpa/LICENSE

%changelog
* Sat Nov 01 2026 linx3141 - 1.0.0-1
- Initial package
