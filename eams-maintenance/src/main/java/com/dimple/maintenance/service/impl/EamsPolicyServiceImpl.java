package com.dimple.maintenance.service.impl;

import com.dimple.common.constant.UserConstants;
import com.dimple.common.core.domain.Ztree;
import com.dimple.common.exception.BusinessException;
import com.dimple.common.utils.StringUtils;
import com.dimple.maintenance.domain.Policy;
import com.dimple.maintenance.mapper.EamsPolicyMapper;
import com.dimple.maintenance.service.EamsPolicyService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * 策略表(Policy)表服务实现类
 *
 * @author makejava
 * @since 2019-04-17 11:35:26
 */
@Service
public class EamsPolicyServiceImpl implements EamsPolicyService {

    @Autowired
    private EamsPolicyMapper policyMapper;

    @Override
    public Policy selectPolicyById(Long polId) {
        return policyMapper.selectPolicyById(polId);
    }


    @Override
    public List<Policy> selectPolicyList(Policy policy) {
        return policyMapper.selectPolicyList(policy);
    }

    @Override
    public int insertPolicy(Policy policy) {
        Policy parentPolicy = policyMapper.selectPolicyById(policy.getParentId());
        //如果父节点不为正常状态，不允许增加
        if (!UserConstants.POLICY_NORMAL.equals(parentPolicy.getStatus())) {
            throw new BusinessException("策略停用，不允许新增");
        }
        //设置访问列表
        policy.setAncestors(parentPolicy.getAncestors() + "," + policy.getParentId());
        return policyMapper.insertPolicy(policy);
    }

    @Override
    public int updatePolicy(Policy policy) {
        Policy parentPolicy = policyMapper.selectPolicyById(policy.getParentId());
        if (StringUtils.isNotNull(parentPolicy)) {
            String ancestors = parentPolicy.getAncestors() + "," + parentPolicy.getPolId();
            policy.setAncestors(ancestors);
            updatePolicyChildren(policy.getPolId(), ancestors);
        }
        return policyMapper.updatePolicy(policy);
    }

    /**
     * 更新子策略的访问路径
     *
     * @param polId     id
     * @param ancestors 访问路径
     */
    private void updatePolicyChildren(Long polId, String ancestors) {
        Policy policy = new Policy();
        policy.setParentId(polId);
        List<Policy> childrens = policyMapper.selectPolicyList(policy);
        for (Policy children : childrens) {
            children.setAncestors(ancestors + "," + policy.getParentId());
        }
        if (childrens.size() > 0) {
            policyMapper.updatePolicyChildren(childrens);
        }
    }

    @Override
    public int deletePolicyById(Long polId) {
        int count = policyMapper.selectPolicyCountByParentId(polId);
        if (count > 0) {
            throw new BusinessException("当前策略存在" + count + "个子策略，不允许删除");
        }
        return policyMapper.deletePolicyById(polId);
    }

    @Override
    public int selectPolicyCountById(Long polId) {
        return policyMapper.selectPolicyCountByParentId(polId);
    }

    @Override
    public List<Ztree> selectPolicyTree(Policy policy) {
        List<Policy> deptList = policyMapper.selectPolicyList(policy);
        List<Ztree> ztrees = initZtree(deptList);
        return ztrees;
    }

    private List<Ztree> initZtree(List<Policy> policyList) {
        return initZtree(policyList, null);
    }

    private List<Ztree> initZtree(List<Policy> policyList, Object o) {
        List<Ztree> ztrees = new ArrayList<Ztree>();
        //boolean isCheck = StringUtils.isNotNull(roleDeptList);
        for (Policy policy : policyList) {
            if (UserConstants.DEPT_NORMAL.equals(policy.getStatus())) {
                Ztree ztree = new Ztree();
                ztree.setId(policy.getPolId());
                ztree.setpId(policy.getParentId());
                ztree.setName(policy.getPolName());
                ztree.setTitle(policy.getPolName());
                //if (isCheck) {
                //    ztree.setChecked(roleDeptList.contains(dept.getDeptId() + dept.getDeptName()));
                //}
                ztrees.add(ztree);
            }
        }
        return ztrees;
    }
}