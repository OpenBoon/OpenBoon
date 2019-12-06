import ProjectSwitcher from '../ProjectSwitcher'
import { colors } from '../Styles'
import LogoSvg from './logo.svg'

const HEIGHT = 40
const LOGO_WIDTH = 110

const LayoutNavBar = () => {
  return (
    <div
      css={{
        display: 'flex',
        justifyContent: 'left',
        alignItems: 'center',
        height: HEIGHT,
        backgroundColor: colors.grey1,
      }}>
      <LogoSvg width={LOGO_WIDTH} />
      <ProjectSwitcher />
    </div>
  )
}

export default LayoutNavBar
