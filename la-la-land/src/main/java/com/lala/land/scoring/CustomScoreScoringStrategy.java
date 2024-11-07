package com.lala.land.scoring;

import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.lala.land.model.dto.question.QuestionContentDTO;
import com.lala.land.model.entity.App;
import com.lala.land.model.entity.Question;
import com.lala.land.model.entity.ScoringResult;
import com.lala.land.model.entity.UserAnswer;
import com.lala.land.model.vo.QuestionVO;
import com.lala.land.service.QuestionService;
import com.lala.land.service.ScoringResultService;

import javax.annotation.Resource;
import java.util.List;
import java.util.Optional;

@ScoringStrategyConfig(appType = 0,scoringStrategy = 0)
public class CustomScoreScoringStrategy implements ScoringStrategy {
    @Resource
    private QuestionService questionService;
    @Resource
    private ScoringResultService scoringResultService;

    @Override
    public UserAnswer doScore(List<String> choices, App app) throws Exception {
        //1.根据id查询到题目和题目结果信息(按得分降序排列)
        Long appId = app.getId();
        Question question = questionService.getOne(
                Wrappers.lambdaQuery(Question.class)
                        .eq(Question::getAppId, appId));

        List<ScoringResult> scoringResultList = scoringResultService.list(
                Wrappers.lambdaQuery(ScoringResult.class)
                        .eq(ScoringResult::getAppId, appId)
                        .orderByDesc(ScoringResult::getResultScoreRange));

        //2.统计用户总得分
        int totalScore = 0;
        QuestionVO questionVO = QuestionVO.objToVo(question);
        List<QuestionContentDTO> questionContent = questionVO.getQuestionContent();
        for(QuestionContentDTO questionContentDTO : questionContent){
            for(String choice : choices){
                for (QuestionContentDTO.Option option : questionContentDTO.getOptions()){
                    if(option.getKey().equals(choice)){
                        int score = Optional.of(option.getScore()).orElse(0);
                        totalScore += score;
                    }
                }
            }
        }
        //3.遍历得分结果，找到第一个用户分数大于得分范围的结果，作为最终结果
        ScoringResult maxScoreResult = scoringResultList.get(0);
        for (ScoringResult scoringResult : scoringResultList) {
            if (totalScore >= scoringResult.getResultScoreRange()) {
                maxScoreResult = scoringResult;
                break;
            }
        }
        //4.构造返回值，填充答案对象的属性。
        UserAnswer userAnswer = new UserAnswer();
        userAnswer.setAppId(appId);
        userAnswer.setAppType(app.getAppType());
        userAnswer.setScoringStrategy(app.getScoringStrategy());
        userAnswer.setChoices(JSONUtil.toJsonStr(choices));
        userAnswer.setResultId(maxScoreResult.getId());
        userAnswer.setResultName(maxScoreResult.getResultName());
        userAnswer.setResultDesc(maxScoreResult.getResultDesc());
        userAnswer.setResultPicture(maxScoreResult.getResultPicture());
        userAnswer.setResultScore(totalScore);
        return null;
    }
}
