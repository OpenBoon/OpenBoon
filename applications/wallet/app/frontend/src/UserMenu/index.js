import { useRef, useState } from 'react'
import PropTypes from 'prop-types'

import { colors, typography, spacing, constants, zIndex } from '../Styles'

import { onBlur as onBlurHelper } from './helpers'

const SIZE = 28

const UserMenu = ({ logout }) => {
  const container = useRef(null)

  const [isMenuOpen, setMenuOpen] = useState(false)

  const onBlur = onBlurHelper({ container, setMenuOpen })

  return (
    <div ref={container} css={{ position: 'relative' }}>
      <button
        type="button"
        onBlur={onBlur}
        onClick={() => setMenuOpen(!isMenuOpen)}
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
        DT
      </button>
      {isMenuOpen && (
        <div
          css={{
            position: 'absolute',
            zIndex: zIndex.reset,
            backgroundColor: colors.rocks.iron,
            top: SIZE + spacing.base,
            right: 0,
            borderRadius: constants.borderRadius.small,
            boxShadow: constants.boxShadows.menu,
          }}>
          <div
            css={{
              padding: spacing.normal,
              borderBottom: constants.borders.separator,
            }}>
            <div css={{ fontWeight: typography.weight.bold }}>
              Danny Tiesling
            </div>
            <div>danny@zorroa.com</div>
          </div>
          <div
            css={{
              padding: spacing.normal,
              display: 'flex',
              flexDirection: 'column',
            }}>
            <a
              href="/"
              onBlur={onBlur}
              css={{
                ':hover': { textDecoration: 'none', color: colors.green2 },
              }}>
              Manage Account
            </a>
            <div css={{ height: spacing.moderate / 2 }} />
            <button
              type="button"
              onBlur={onBlur}
              onClick={logout}
              css={{
                border: 0,
                margin: 0,
                padding: 0,
                backgroundColor: 'transparent',
                fontSize: 'inherit',
                textAlign: 'inherit',
                color: 'inherit',
                ':hover': {
                  color: colors.green2,
                  cursor: 'pointer',
                },
              }}>
              Sign Out
            </button>
          </div>
        </div>
      )}
    </div>
  )
}

UserMenu.propTypes = {
  logout: PropTypes.func.isRequired,
}

export default UserMenu
