import { spacing, colors, constants, typography } from '../Styles'

import ButtonCopy from '../Button/Copy'

const ProjectUsersAddCopyLink = () => {
  const LINK = `${window.location.origin}/create-account`

  return (
    <div css={{ paddingTop: spacing.moderate }}>
      <label
        htmlFor="copyLink"
        css={{
          display: 'block',
          color: colors.structure.steel,
        }}
      >
        A user must have an account to be added. Accounts can be created here:
      </label>
      <div
        css={{
          display: 'flex',
          alignItems: 'center',
          paddingTop: spacing.normal,
        }}
      >
        <input
          id="copyLink"
          type="text"
          value={LINK}
          readOnly
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
          }}
        >
          <ButtonCopy title="Link" value={LINK} offset={50} />
        </div>
      </div>
    </div>
  )
}

export default ProjectUsersAddCopyLink
