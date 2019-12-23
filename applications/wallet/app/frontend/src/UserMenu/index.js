import PropTypes from 'prop-types'

import userShape from '../User/shape'

import { colors, typography, spacing, constants } from '../Styles'

import ChevronSvg from '../Icons/chevron.svg'

import Menu from '../Menu'
import Button, { VARIANTS } from '../Button'

const SIZE = 28
const CHEVRON_WIDTH = 20

const UserMenu = ({ user: { firstName, lastName, email }, logout }) => {
  return (
    <div css={{ marginRight: -spacing.moderate }}>
      <Menu
        open="left"
        button={({ onBlur, onClick, isMenuOpen }) => (
          <Button
            aria-label="Open user menu"
            variant={VARIANTS.MENU}
            style={{
              [isMenuOpen && 'backgroundColor']: colors.structure.smoke,
              ':hover': {
                cursor: 'pointer',
              },
            }}
            onBlur={onBlur}
            onClick={onClick}
            isDisabled={false}>
            <div css={{ display: 'flex' }}>
              <div
                css={{
                  display: 'flex',
                  justifyContent: 'center',
                  alignItems: 'center',
                  border: 0,
                  margin: 0,
                  padding: 0,
                  width: SIZE,
                  height: SIZE,
                  borderRadius: SIZE,
                  color: isMenuOpen
                    ? colors.structure.white
                    : colors.structure.lead,
                  backgroundColor: isMenuOpen
                    ? colors.structure.lead
                    : colors.structure.steel,
                  fontWeight: typography.weight.bold,
                }}>
                {`${firstName ? firstName[0] : 'O'}${
                  lastName ? lastName[0] : 'P'
                }`}
              </div>
              <ChevronSvg
                width={CHEVRON_WIDTH}
                color={colors.structure.steel}
                css={{
                  marginLeft: spacing.base,
                  transform: `${isMenuOpen ? 'rotate(-180deg)' : ''}`,
                }}
              />
            </div>
          </Button>
        )}>
        {({ onBlur }) => (
          <div>
            <div
              css={{
                padding: spacing.normal,
                paddingBottom: spacing.comfy,
                borderBottom: constants.borders.separator,
              }}>
              <div css={{ fontWeight: typography.weight.bold }}>
                {`${firstName} ${lastName}`}
              </div>
              <div>{email}</div>
            </div>
            <ul>
              <li>
                <Button
                  variant={VARIANTS.MENU_ITEM}
                  style={{
                    padding: `${spacing.base} 0`,
                    borderRadius: 0,
                    borderBottom: constants.borders.transparent,
                  }}
                  onBlur={onBlur}
                  onClick={logout}
                  isDisabled={false}>
                  Sign Out
                </Button>
              </li>
            </ul>
          </div>
        )}
      </Menu>
    </div>
  )
}

UserMenu.propTypes = {
  user: PropTypes.shape(userShape).isRequired,
  logout: PropTypes.func.isRequired,
}

export default UserMenu
