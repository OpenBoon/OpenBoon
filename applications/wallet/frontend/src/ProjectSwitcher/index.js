import PropTypes from 'prop-types'
import { useRouter } from 'next/router'
import useSWR from 'swr'
import Link from 'next/link'

import { colors, constants, spacing } from '../Styles'

import ChevronSvg from '../Icons/chevron.svg'

import { useLocalStorage } from '../LocalStorage/helpers'

import Menu from '../Menu'
import Button, { VARIANTS } from '../Button'

const ProjectSwitcher = ({ projectId }) => {
  const {
    pathname,
    query: { projectId: routerProjectId },
    asPath,
  } = useRouter()

  const { data: { results: projects = [] } = {} } = useSWR('/api/v1/projects/')

  const [sortBy] = useLocalStorage({
    key: 'AccountContent.sortBy',
    initialState: 'name',
  })

  if (!routerProjectId) return null

  if (!projectId || !projects.length === 0) return null

  const selectedProject = projects.find(({ id }) => id === projectId)

  if (!selectedProject) return null

  if (projects.length === 1) {
    return (
      <div
        css={{
          display: 'flex',
          justifyContent: 'center',
          alignItems: 'center',
          padding: `${spacing.small}px ${spacing.base}px`,
          color: colors.key.two,
        }}
      >
        {selectedProject.name}
      </div>
    )
  }

  return (
    <Menu
      open="bottom-right"
      button={({ onBlur, onClick, isMenuOpen }) => (
        <Button variant={VARIANTS.MENU} onBlur={onBlur} onClick={onClick}>
          <div
            css={{
              display: 'flex',
              justifyContent: 'center',
              alignItems: 'center',
            }}
          >
            {selectedProject.name}
            <ChevronSvg
              height={constants.icons.regular}
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
        <ul>
          {[...projects]
            .sort((a, b) => {
              switch (sortBy) {
                case 'date': {
                  if (a.createdDate > b.createdDate) return -1
                  if (a.createdDate < b.createdDate) return 1
                  return 0
                }

                case 'name':
                default: {
                  if (a.name.toLowerCase() < b.name.toLowerCase()) return -1
                  if (a.name.toLowerCase() > b.name.toLowerCase()) return 1
                  return 0
                }
              }
            })
            .map(({ id, name }) => (
              <li key={id}>
                <Link
                  href={
                    id === projectId
                      ? pathname
                      : pathname.split('/').slice(0, 3).join('/')
                  }
                  as={
                    id === projectId
                      ? asPath
                      : pathname
                          .replace('[projectId]', id)
                          .split('/')
                          .slice(0, 3)
                          .join('/')
                  }
                  passHref
                >
                  <Button
                    variant={VARIANTS.MENU_ITEM}
                    onBlur={onBlur}
                    onClick={onClick}
                  >
                    {name}
                  </Button>
                </Link>
              </li>
            ))}
        </ul>
      )}
    </Menu>
  )
}

ProjectSwitcher.propTypes = {
  projectId: PropTypes.string.isRequired,
}

export default ProjectSwitcher
