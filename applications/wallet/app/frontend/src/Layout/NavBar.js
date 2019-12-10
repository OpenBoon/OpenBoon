import PropTypes from 'prop-types'

import { colors, spacing, constants, zIndex } from '../Styles'

import LogoSvg from '../Icons/logo.svg'

import ProjectSwitcher from '../ProjectSwitcher'

const LOGO_WIDTH = 110

const LayoutNavBar = ({
  isToolDrawerOpen,
  projects,
  setToolDrawerOpen,
  setSelectedProject,
}) => {
  return (
    <div
      css={{
        position: 'fixed',
        top: 0,
        left: 0,
        right: 0,
        height: constants.navbar.height,
        display: 'flex',
        justifyContent: 'left',
        alignItems: 'center',
        backgroundColor: colors.grey1,
        padding: spacing.small,
        boxShadow: constants.boxShadows.navBar,
        zIndex: zIndex.layout.navbar,
      }}>
      <button
        type="button"
        onClick={() => setToolDrawerOpen(!isToolDrawerOpen)}>
        Hamburger
      </button>
      <LogoSvg width={LOGO_WIDTH} />
      <ProjectSwitcher
        projects={projects}
        setSelectedProject={setSelectedProject}
      />
    </div>
  )
}

LayoutNavBar.propTypes = {
  isToolDrawerOpen: PropTypes.bool.isRequired,
  projects: PropTypes.arrayOf(
    PropTypes.shape({
      id: PropTypes.string.isRequired,
      name: PropTypes.string.isRequired,
    }),
  ).isRequired,
  setToolDrawerOpen: PropTypes.func.isRequired,
  setSelectedProject: PropTypes.func.isRequired,
}

export default LayoutNavBar
