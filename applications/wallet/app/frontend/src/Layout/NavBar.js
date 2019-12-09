import PropTypes from 'prop-types'

import ProjectSwitcher from '../ProjectSwitcher'
import { colors, spacing } from '../Styles'
import LogoSvg from './logo.svg'

const LOGO_WIDTH = 110

const LayoutNavBar = ({ projects, setSelectedProject }) => {
  return (
    <div
      css={{
        display: 'flex',
        justifyContent: 'left',
        alignItems: 'center',
        backgroundColor: colors.grey1,
        padding: spacing.small,
      }}>
      <LogoSvg width={LOGO_WIDTH} />
      <ProjectSwitcher
        projects={projects}
        setSelectedProject={setSelectedProject}
      />
    </div>
  )
}

LayoutNavBar.propTypes = {
  projects: PropTypes.arrayOf(
    PropTypes.shape({
      id: PropTypes.string.isRequired,
      name: PropTypes.string.isRequired,
    }),
  ).isRequired,
  setSelectedProject: PropTypes.func.isRequired,
}

export default LayoutNavBar
