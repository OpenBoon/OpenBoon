import PropTypes from 'prop-types'
import Link from 'next/link'

import { colors, spacing } from '../Styles'

import LogoSvg from '../Icons/logo.svg'

import ProjectSwitcher from '../ProjectSwitcher'

import HamburgerSvg from './hamburger.svg'

import NavbarWrapper from './Wrapper'

const LOGO_WIDTH = 110

const Navbar = ({ projectId, isSidebarOpen, setSidebarOpen, children }) => {
  return (
    <NavbarWrapper>
      <div css={{ display: 'flex', alignItems: 'stretch' }}>
        {!!projectId && (
          <button
            aria-label="Open Sidebar Menu"
            type="button"
            onClick={() => setSidebarOpen(!isSidebarOpen)}
            css={{
              border: 0,
              backgroundColor: 'inherit',
              color: colors.structure.steel,
              display: 'flex',
              justifyContent: 'center',
              alignItems: 'center',
              padding: spacing.base,
              margin: 0,
              marginLeft: -spacing.base,
              cursor: 'pointer',
            }}>
            <HamburgerSvg width={20} aria-hidden />
          </button>
        )}

        <Link href="/" passHref>
          <a
            css={{ paddingLeft: spacing.base, paddingRight: spacing.base }}
            aria-label="Home">
            <LogoSvg width={LOGO_WIDTH} />
          </a>
        </Link>

        <ProjectSwitcher projectId={projectId} />
      </div>

      {children}
    </NavbarWrapper>
  )
}

Navbar.propTypes = {
  projectId: PropTypes.string.isRequired,
  isSidebarOpen: PropTypes.bool.isRequired,
  setSidebarOpen: PropTypes.func.isRequired,
  children: PropTypes.node.isRequired,
}

export default Navbar
