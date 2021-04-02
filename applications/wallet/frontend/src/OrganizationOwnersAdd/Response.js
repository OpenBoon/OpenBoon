import PropTypes from 'prop-types'
import Link from 'next/link'

import { colors, spacing } from '../Styles'

import FlashMessage, { VARIANTS as FLASH_VARIANTS } from '../FlashMessage'
import Button, { VARIANTS as BUTTON_VARIANTS } from '../Button'
import ButtonGroup from '../Button/Group'
import SectionTitle from '../SectionTitle'

import ProjectUsersAddCopyLink from '../ProjectUsersAdd/CopyLink'

const OrganizationOwnersAddResponse = ({
  organizationId,
  succeeded,
  failed,
  onReset,
}) => {
  return (
    <div>
      {failed.length > 0 && (
        <>
          <div
            css={{
              display: 'flex',
              paddingTop: spacing.base,
              paddingBottom: spacing.base,
            }}
          >
            <FlashMessage variant={FLASH_VARIANTS.ERROR}>
              Owner(s) not added.
            </FlashMessage>
          </div>

          <SectionTitle>Owner(s) that Need an Account</SectionTitle>

          <div
            css={{
              paddingTop: spacing.moderate,
              paddingBottom: spacing.moderate,
              color: colors.structure.steel,
            }}
          >
            {failed.join(', ')}
          </div>

          <ProjectUsersAddCopyLink />
        </>
      )}

      {succeeded.length > 0 && (
        <>
          <div
            css={{
              display: 'flex',
              paddingTop: spacing.base,
              paddingBottom: spacing.base,
            }}
          >
            <FlashMessage variant={FLASH_VARIANTS.SUCCESS}>
              Owner(s) added.
            </FlashMessage>
          </div>

          <SectionTitle>Owner(s) Added</SectionTitle>

          <div
            css={{
              paddingTop: spacing.moderate,
              color: colors.structure.steel,
            }}
          >
            {succeeded.join(', ')}
          </div>
        </>
      )}

      <ButtonGroup>
        <Button variant={BUTTON_VARIANTS.SECONDARY} onClick={onReset}>
          Add More Owner(s)
        </Button>

        <Link href={`/organizations/${organizationId}/owners`} passHref>
          <Button variant={BUTTON_VARIANTS.PRIMARY}>View All</Button>
        </Link>
      </ButtonGroup>
    </div>
  )
}

OrganizationOwnersAddResponse.propTypes = {
  organizationId: PropTypes.string.isRequired,
  succeeded: PropTypes.arrayOf(PropTypes.string.isRequired).isRequired,
  failed: PropTypes.arrayOf(PropTypes.string.isRequired).isRequired,
  onReset: PropTypes.func.isRequired,
}

export default OrganizationOwnersAddResponse
