import ProjectSwitcher from '../ProjectSwitcher'
import { colors, spacing } from '../Styles'
import LogoSvg from './logo.svg'

const LOGO_WIDTH = 110

const LayoutNavBar = () => {
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
      <ProjectSwitcher />
    </div>
  )
}

export default LayoutNavBar
