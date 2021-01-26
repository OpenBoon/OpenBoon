import PropTypes from 'prop-types'
import Link from 'next/link'

import { colors, spacing, constants } from '../Styles'

import Button, { VARIANTS } from '../Button'

import DataSourcesSvg from '../Icons/datasources.svg'
import JobQueueSvg from '../Icons/jobQueue.svg'
import ModelsSvg from '../Icons/models.svg'
import VisualizerSvg from '../Icons/visualizer.svg'
import KeySvg from '../Icons/key.svg'
import UsersSvg from '../Icons/users.svg'

const MAX_WIDTH = 250

const LINKS = [
  {
    url: 'data-sources',
    title: 'Data Sources',
    icon: <DataSourcesSvg height={constants.icons.regular} />,
    legend:
      'Connect to the remote repository that contains the files to be imported and specify the types of analyses to be applied to them.',
  },
  {
    url: 'jobs',
    title: 'Job Queue',
    icon: <JobQueueSvg height={constants.icons.regular} />,
    legend:
      'Monitor and manage the processing import and module application jobs, and review and troubleshoot any issues.',
  },
  {
    url: 'models',
    title: 'Custom Models',
    icon: <ModelsSvg height={constants.icons.regular} />,
    legend: 'Create, manage, and view the performance of your custom models. ',
  },
  {
    url: 'visualizer',
    title: 'Visualizer',
    icon: <VisualizerSvg height={constants.icons.regular} />,
    legend:
      'View the results of the analysis and inspect and tune the metadata output.',
  },
  {
    url: 'api-keys',
    title: 'API Keys',
    icon: <KeySvg height={constants.icons.regular} />,
    legend:
      'Create a ZMLP API key, which allows you to obtain access to ZVI API resources.',
  },
  {
    url: 'users',
    title: 'User Admin',
    icon: <UsersSvg height={constants.icons.regular} />,
    legend: 'Add users to your project and manage their permissions.',
  },
]

const ProjectQuickLinks = ({ projectId }) => {
  return (
    <div
      css={{
        display: 'flex',
        flexDirection: 'column',
        backgroundColor: colors.structure.smoke,
        boxShadow: constants.boxShadows.tableRow,
        borderRadius: constants.borderRadius.small,
        padding: spacing.comfy,
      }}
    >
      <h3 css={{ paddingBottom: spacing.base }}>Quick Links</h3>
      <div
        css={{
          display: 'grid',
          gap: spacing.spacious,
          gridTemplateColumns: `repeat(auto-fill, minmax(${MAX_WIDTH}px, 1fr))`,
        }}
      >
        {LINKS.map((link) => {
          return (
            <div
              key={link.url}
              css={{ display: 'flex', flexDirection: 'column' }}
            >
              <div css={{ display: 'flex', paddingBottom: spacing.base }}>
                <Link href={`${projectId}/${link.url}`} passHref>
                  <Button variant={VARIANTS.SECONDARY_SMALL}>
                    <div css={{ display: 'flex', alignItems: 'center' }}>
                      <div
                        css={{
                          display: 'flex',
                          paddingRight: spacing.small,
                        }}
                      >
                        {link.icon}
                      </div>
                      {link.title}
                    </div>
                  </Button>
                </Link>
              </div>
              {link.legend}
            </div>
          )
        })}
      </div>
    </div>
  )
}

ProjectQuickLinks.propTypes = {
  projectId: PropTypes.string.isRequired,
}

export default ProjectQuickLinks
