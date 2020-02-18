import { useRef } from 'react'

import { spacing, colors, constants, typography } from '../Styles'

import Button, { VARIANTS } from '../Button'

import { onCopy } from './helpers'

const ProjectUsersAddCopyLink = () => {
  const inputRef = useRef()

  return (
    <div css={{ paddingTop: spacing.moderate }}>
      <label
        htmlFor="copyLink"
        css={{
          display: 'block',
          color: colors.structure.steel,
        }}>
        A user must have an account to be added. Accounts can be created here:
      </label>
      <div
        css={{
          display: 'flex',
          alignItems: 'center',
          paddingTop: spacing.normal,
        }}>
        <input
          ref={inputRef}
          id="copyLink"
          type="text"
          defaultValue={`${window.location.hostname}/create-account`}
          css={{
            width: constants.form.maxWidth,
            fontSize: typography.size.regular,
            lineHeight: typography.height.regular,
            color: colors.structure.white,
            backgroundColor: colors.structure.mattGrey,
            borderRadius: constants.borderRadius.small,
            padding: spacing.moderate,
            border: 'none',
            resize: 'none',
          }}
        />
        <div
          css={{
            display: 'flex',
            alignItems: 'center',
            paddingLeft: spacing.small,
            paddingRight: spacing.small,
          }}>
          <Button
            variant={VARIANTS.LINK}
            style={{
              '&:active': {
                opacity: 0,
                transition: 'opacity .3s ease',
              },
            }}
            onClick={() => onCopy({ inputRef })}>
            Copy Link
          </Button>
        </div>
      </div>
    </div>
  )
}

export default ProjectUsersAddCopyLink
