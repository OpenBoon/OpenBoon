import PropTypes from 'prop-types'
import Link from 'next/link'

import userShape from '../User/shape'

import { colors, typography, spacing, constants } from '../Styles'

import ChevronSvg from '../Icons/chevron.svg'

import Menu from '../Menu'
import Button, { VARIANTS } from '../Button'

const SIZE = 28

const UserMenu = ({ user: { firstName, lastName, email }, logout }) => {
  return (
    <div css={{ marginRight: -spacing.moderate }}>
      <Menu
        open="bottom-left"
        button={({ onBlur, onClick, isMenuOpen }) => (
          <Button
            aria-label="Open user menu"
            variant={VARIANTS.MENU}
            style={{
              ...(isMenuOpen
                ? { backgroundColor: colors.structure.smoke }
                : {}),
              ':hover': {
                cursor: 'pointer',
              },
            }}
            onBlur={onBlur}
            onClick={onClick}
          >
            <div css={{ display: 'flex', alignItems: 'center' }}>
              <div
                css={{
                  display: 'flex',
                  justifyContent: 'center',
                  alignItems: 'center',
                  border: 0,
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
                }}
              >
                {`${firstName ? firstName[0] : ''}${
                  lastName ? lastName[0] : ''
                }`}
              </div>
              <ChevronSvg
                height={constants.icons.regular}
                color={colors.structure.steel}
                css={{
                  marginLeft: spacing.base,
                  transform: `${isMenuOpen ? 'rotate(-180deg)' : ''}`,
                }}
              />
            </div>
          </Button>
        )}
      >
        {({ onBlur, onClick }) => (
          <div>
            <div
              css={{
                padding: spacing.normal,
                borderBottom: constants.borders.regular.zinc,
              }}
            >
              <div css={{ fontWeight: typography.weight.bold }}>
                {`${firstName} ${lastName}`}
              </div>
              <div>{email}</div>
            </div>
            <ul css={{ borderBottom: constants.borders.regular.zinc }}>
              <li>
                <Link href="/account" passHref>
                  <Button
                    variant={VARIANTS.MENU_ITEM}
                    onBlur={onBlur}
                    onClick={onClick}
                  >
                    Manage Account
                  </Button>
                </Link>
              </li>
              <li>
                <Button
                  href="mailto:support@boonai.io"
                  target="_blank"
                  variant={VARIANTS.MENU_ITEM}
                  onBlur={onBlur}
                  onClick={(event) => {
                    onClick(event)
                  }}
                >
                  Contact Support
                </Button>
              </li>
              <li>
                <Button
                  href="https://shipright.community/zorroa"
                  target="_blank"
                  variant={VARIANTS.MENU_ITEM}
                  onBlur={onBlur}
                  onClick={onClick}
                >
                  Submit Feedback
                </Button>
              </li>
            </ul>
            <ul>
              <li>
                <Button
                  variant={VARIANTS.MENU_ITEM}
                  onBlur={onBlur}
                  onClick={() => logout({ redirectUrl: '/', redirectAs: '/' })}
                >
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
