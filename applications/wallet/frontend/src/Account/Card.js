import Link from 'next/link'
import PropTypes from 'prop-types'

import { colors, spacing, constants, typography } from '../Styles'

import Button, { VARIANTS } from '../Button'

import Feature, { ENVS } from '../Feature'

import DataSourcesSvg from '../Icons/datasources.svg'
import JobQueueSvg from '../Icons/jobQueue.svg'
import ModelsSvg from '../Icons/models.svg'
import VisualizerSvg from '../Icons/visualizer.svg'
import WebhooksSvg from '../Icons/webhooks.svg'
import KeySvg from '../Icons/key.svg'
import UsersSvg from '../Icons/users.svg'

const LINKS = [
  {
    url: 'data-sources',
    title: 'Data Sources',
    icon: <DataSourcesSvg height={constants.icons.regular} />,
  },
  {
    url: 'jobs',
    title: 'Job Queue',
    icon: <JobQueueSvg height={constants.icons.regular} />,
  },
  {
    url: 'models',
    title: 'Custom Models',
    icon: <ModelsSvg height={constants.icons.regular} />,
  },
  {
    url: 'visualizer',
    title: 'Visualizer',
    icon: <VisualizerSvg height={constants.icons.regular} />,
  },
  {
    url: 'webhooks',
    title: 'Webhooks',
    icon: <WebhooksSvg height={constants.icons.regular} />,
    flag: 'webhooks-feature-flag',
    envs: [ENVS.DEV],
  },
  {
    url: 'api-keys',
    title: 'API Keys',
    icon: <KeySvg height={constants.icons.regular} />,
  },
  {
    url: 'users',
    title: 'User Admin',
    icon: <UsersSvg height={constants.icons.regular} />,
  },
]

const AccountCard = ({ projectId, name, organizationName }) => {
  return (
    <div
      css={{
        display: 'flex',
        flexDirection: 'column',
        backgroundColor: colors.structure.smoke,
        boxShadow: constants.boxShadows.tableRow,
        borderRadius: constants.borderRadius.medium,
        padding: spacing.comfy,
      }}
    >
      <h3
        title={name}
        css={{
          paddingBottom: spacing.base,
          overflow: 'hidden',
          whiteSpace: 'nowrap',
          textOverflow: 'ellipsis',
          fontSize: typography.size.giant,
          lineHeight: typography.height.giant,
        }}
      >
        {name}
      </h3>

      <div
        css={{
          color: colors.structure.zinc,
          fontFamily: typography.family.condensed,
          fontWeight: typography.weight.regular,
        }}
      >
        Organization: {organizationName}
      </div>

      <div
        css={{
          color: colors.structure.zinc,
          paddingBottom: spacing.normal,
          fontFamily: typography.family.condensed,
          fontWeight: typography.weight.regular,
        }}
      >
        Project ID:&nbsp;
        <span css={{ textTransform: 'uppercase' }}>{projectId}</span>
      </div>

      <div css={{ display: 'flex' }}>
        <Link href="/[projectId]" as={`/${projectId}`} passHref>
          <Button variant={VARIANTS.PRIMARY_SMALL}>
            Go To Project Dashboard
          </Button>
        </Link>
      </div>

      <div
        css={{
          color: colors.structure.zinc,
          paddingTop: spacing.comfy,
          paddingBottom: spacing.base,
          fontFamily: typography.family.condensed,
          fontWeight: typography.weight.regular,
          textTransform: 'uppercase',
        }}
      >
        Quick Links
      </div>

      <div css={{ display: 'flex', gap: spacing.normal }}>
        {LINKS.map(
          ({
            url,
            title,
            icon,
            flag = '',
            envs = [...Object.values(ENVS)],
          }) => {
            return (
              <Feature key={url} flag={flag} envs={envs}>
                <Link href={`/${projectId}/${url}`} passHref>
                  <Button
                    title={title}
                    aria-label={title}
                    variant={VARIANTS.ICON}
                    css={{
                      color: colors.structure.white,
                      '&,&:hover,&:visited': {
                        backgroundColor: colors.structure.steel,
                      },
                      '&:hover': {
                        backgroundColor: colors.structure.zinc,
                      },
                    }}
                  >
                    {icon}
                  </Button>
                </Link>
              </Feature>
            )
          },
        )}
      </div>
    </div>
  )
}

AccountCard.propTypes = {
  projectId: PropTypes.string.isRequired,
  name: PropTypes.string.isRequired,
  organizationName: PropTypes.string.isRequired,
}

export default AccountCard
