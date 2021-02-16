import { useState, useContext } from 'react'

import { colors, constants, typography, spacing } from '../Styles'

import { UserContext } from '../User'

import FlashMessage, { VARIANTS as FLASH_VARIANTS } from '../FlashMessage'
import Button, { VARIANTS as BUTTON_VARIANTS } from '../Button'
import ButtonGroup from '../Button/Group'

import { onReset } from './helpers'

const AccountPasswordNotice = () => {
  const [error, setError] = useState('')

  const {
    user: { email },
    googleAuth,
  } = useContext(UserContext)

  return (
    <div
      css={{
        paddingTop: spacing.comfy,
        paddingLeft: spacing.giant,
      }}
    >
      {error && (
        <div css={{ display: 'flex', paddingBottom: spacing.moderate }}>
          <FlashMessage variant={FLASH_VARIANTS.ERROR}>{error}</FlashMessage>
        </div>
      )}
      <div
        css={{
          width: constants.form.maxWidth,
          border: constants.borders.regular.smoke,
          borderRadius: constants.borderRadius.small,
          padding: spacing.spacious,
        }}
      >
        <div
          css={{
            color: colors.structure.white,
            fontWeight: typography.weight.medium,
            paddingBottom: spacing.small,
          }}
        >
          Did you sign in with Google or another service?
        </div>

        <div
          css={{
            color: colors.structure.steel,
            fontSize: typography.size.regular,
            lineHeight: typography.height.regular,
            fontWeight: typography.weight.regular,
          }}
        >
          If so, you may not have a Boon AI password. You can create one by
          resetting the password and checking your email for instructions.
        </div>

        <ButtonGroup>
          <Button
            variant={BUTTON_VARIANTS.PRIMARY_SMALL}
            onClick={() => onReset({ setError, email, googleAuth })}
          >
            Reset Password
          </Button>
        </ButtonGroup>
      </div>
    </div>
  )
}

export default AccountPasswordNotice
