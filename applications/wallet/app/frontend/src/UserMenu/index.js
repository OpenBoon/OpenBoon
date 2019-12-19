import PropTypes from 'prop-types'

import userShape from '../User/shape'

import { colors, typography, spacing, constants } from '../Styles'

import Menu from '../Menu'
import Button, { VARIANTS } from '../Button'

const SIZE = 28

const UserMenu = ({
  user: { first_name: firstName, last_name: lastName, email },
  logout,
}) => {
  return (
    <Menu
      button={({ onBlur, onClick }) => (
        <button
          type="button"
          onBlur={onBlur}
          onClick={onClick}
          css={{
            border: 0,
            margin: 0,
            padding: 0,
            width: SIZE,
            height: SIZE,
            borderRadius: SIZE,
            color: colors.rocks.white,
            backgroundColor: colors.rocks.charcoal,
            fontWeight: typography.weight.bold,
            ':hover': {
              cursor: 'pointer',
            },
          }}>
          {`${firstName[0]}${lastName[0]}`}
        </button>
      )}>
      {({ onBlur }) => (
        <div>
          <div
            css={{
              padding: spacing.normal,
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
  )
}

UserMenu.propTypes = {
  user: PropTypes.shape(userShape).isRequired,
  logout: PropTypes.func.isRequired,
}

export default UserMenu
